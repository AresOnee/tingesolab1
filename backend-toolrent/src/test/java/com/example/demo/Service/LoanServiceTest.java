package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;

    @InjectMocks private LoanService loanService;

    // -------- helpers --------
    private ClientEntity client(Long id) {
        ClientEntity c = new ClientEntity();
        c.setId(id);
        c.setName("Cliente " + id);
        c.setRut("11.111.111-" + id);
        c.setEmail("c"+id+"@toolrent.cl");
        c.setPhone("+5690000" + id);
        c.setState("Activo");
        return c;
    }

    private ToolEntity tool(Long id, String status, Integer stock) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName("Herr " + id);
        t.setCategory("Cat");
        t.setReplacementValue(1000);
        t.setStock(stock);
        t.setStatus(status);
        return t;
    }

    private LoanEntity activeLoan(Long clientId, Long toolId, LocalDate start, LocalDate due) {
        LoanEntity l = new LoanEntity();
        l.setId(999L);
        l.setClient(client(clientId));
        l.setTool(tool(toolId, "Prestada", 0));
        l.setStartDate(start);
        l.setDueDate(due);
        l.setReturnDate(null);
        l.setStatus("Vigente");
        l.setFine(0.0);
        l.setDamaged(false);
        l.setIrreparable(false);
        return l;
    }

    // ===========================================================
    // Reglas: sin stock / vencidos / máximo 5 / duplicado herramienta
    // ===========================================================

    @Test
    @DisplayName("Regla: rechaza préstamo si la herramienta no está 'Disponible'")
    void rejectWhenToolNotDisponible() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Prestada", 1)));

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no está disponible");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla: rechaza préstamo si stock es 0")
    void rejectWhenNoStock() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 0)));

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sin stock");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla: rechaza si el cliente tiene préstamos vencidos (sin returnDate)")
    void rejectWhenClientHasOverdues() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 2)));

        // un préstamo vencido (dueDate < hoy, returnDate == null)
        List<LoanEntity> all = new ArrayList<>();
        all.add(activeLoan(clientId, 99L, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)));
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("vencidos");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla: máximo 5 préstamos activos por cliente")
    void rejectWhenClientHas5Actives() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 3)));

        // cinco préstamos activos (returnDate == null)
        List<LoanEntity> all = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            all.add(activeLoan(clientId, (long) i, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3)));
        }
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Máximo 5 préstamos");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla: no permitir misma herramienta activa en paralelo para el cliente")
    void rejectDuplicateToolForClient() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 2)));

        // ya existe para este cliente la misma tool sin returnDate
        List<LoanEntity> all = List.of(
                activeLoan(clientId, toolId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))
        );
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("mismo");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Caso feliz: crea préstamo, descuenta stock y guarda")
    void createLoanHappyPath() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(7);

        ClientEntity c = client(clientId);
        ToolEntity   t = tool(toolId, "Disponible", 3);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(c));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(t));
        when(loanRepository.findAll()).thenReturn(List.of()); // sin restricciones
        when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> {
            LoanEntity saved = inv.getArgument(0, LoanEntity.class);
            saved.setId(100L);
            return saved;
        });
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanEntity loan = loanService.createLoan(clientId, toolId, due);

        assertThat(loan.getId()).isNotNull();
        assertThat(loan.getClient().getId()).isEqualTo(clientId);
        assertThat(loan.getTool().getId()).isEqualTo(toolId);
        assertThat(loan.getReturnDate()).isNull();
        assertThat(loan.getDueDate()).isEqualTo(due);
        assertThat(loan.getStatus()).containsIgnoringCase("Vigente");

        // stock descontado
        assertThat(t.getStock()).isEqualTo(2);

        verify(loanRepository).save(any(LoanEntity.class));
        verify(toolRepository).save(any(ToolEntity.class));
    }
}
