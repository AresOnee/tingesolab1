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
import org.springframework.http.HttpStatus;
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
                .hasMessageContaining("misma");

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

    // ========== NUEVOS TESTS PARA CUBRIR LAMBDAS Y VALIDACIONES ==========

    @Test
    @DisplayName("createLoan: debe lanzar 404 cuando cliente no existe (lambda línea 40)")
    void createLoan_clientNotFound_throwsException() {
        // Given
        Long clientId = 999L;
        Long toolId = 1L;
        LocalDate dueDate = LocalDate.now().plusDays(7);

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cliente no encontrado")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(clientRepository).findById(clientId);
        verify(toolRepository, never()).findById(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLoan: debe lanzar 404 cuando herramienta no existe (lambda línea 42)")
    void createLoan_toolNotFound_throwsException() {
        // Given
        Long clientId = 1L;
        Long toolId = 999L;
        LocalDate dueDate = LocalDate.now().plusDays(7);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Herramienta no encontrada")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(clientRepository).findById(clientId);
        verify(toolRepository).findById(toolId);
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando clientId es null")
    void createLoan_nullClientId_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> loanService.createLoan(null, 1L, LocalDate.now().plusDays(7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando toolId es null")
    void createLoan_nullToolId_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> loanService.createLoan(1L, null, LocalDate.now().plusDays(7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando dueDate es null")
    void createLoan_nullDueDate_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> loanService.createLoan(1L, 1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
    }

    @Nested
    @DisplayName("Tests de returnTool (devolución de herramientas)")
    class ReturnToolTests {

        @Test
        @DisplayName("Devolución a tiempo en buen estado: sin multa, stock aumenta, status Devuelto")
        void returnTool_onTime_noDamage() {
            // Given: Un préstamo activo que se devuelve a tiempo
            Long loanId = 100L;
            LocalDate startDate = LocalDate.now().minusDays(5);
            LocalDate dueDate = LocalDate.now().plusDays(2); // aún no vence

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2); // stock actual = 2

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(startDate);
            activeLoan.setDueDate(dueDate);
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);
            activeLoan.setDamaged(false);
            activeLoan.setIrreparable(false);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve la herramienta a tiempo y sin daños
            LoanEntity returned = loanService.returnTool(loanId, false, false);

            // Then:
            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getStatus()).isEqualTo("Devuelto");
            assertThat(returned.getFine()).isEqualTo(0.0); // sin multa
            assertThat(tool.getStock()).isEqualTo(3); // aumentó de 2 a 3
            assertThat(tool.getStatus()).isEqualTo("Disponible");

            verify(loanRepository).save(any(LoanEntity.class));
            verify(toolRepository).save(any(ToolEntity.class));
        }

        @Test
        @DisplayName("Devolución con atraso: genera multa, status Atrasado")
        void returnTool_late_generatesFine() {
            // Given: Un préstamo que ya venció hace 3 días
            Long loanId = 101L;
            LocalDate dueDate = LocalDate.now().minusDays(3); // venció hace 3 días

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 1);

            LoanEntity overdueLoan = new LoanEntity();
            overdueLoan.setId(loanId);
            overdueLoan.setClient(client);
            overdueLoan.setTool(tool);
            overdueLoan.setStartDate(LocalDate.now().minusDays(10));
            overdueLoan.setDueDate(dueDate);
            overdueLoan.setReturnDate(null);
            overdueLoan.setStatus("Atrasado");
            overdueLoan.setFine(0.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(overdueLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve con 3 días de atraso
            LoanEntity returned = loanService.returnTool(loanId, false, false);

            // Then: Debe tener multa de 3.0 (3 días * 1.0 por día)
            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getStatus()).isEqualTo("Atrasado");
            assertThat(returned.getFine()).isEqualTo(3.0); // 3 días de multa
            assertThat(tool.getStock()).isEqualTo(2); // se recuperó el stock
            assertThat(tool.getStatus()).isEqualTo("Disponible");
        }

        @Test
        @DisplayName("Devolución con daño reparable: herramienta va a reparación, no recupera stock")
        void returnTool_damaged_reparable() {
            // Given: Un préstamo activo
            Long loanId = 102L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 3);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(LocalDate.now().minusDays(3));
            activeLoan.setDueDate(LocalDate.now().plusDays(4));
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve dañada pero reparable (damaged=true, irreparable=false)
            LoanEntity returned = loanService.returnTool(loanId, true, false);

            // Then:
            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getStatus()).isEqualTo("Devuelto");
            assertThat(tool.getStatus()).isEqualTo("En reparación");
            assertThat(tool.getStock()).isEqualTo(3); // NO aumenta el stock
            assertThat(returned.getFine()).isEqualTo(0.0); // sin multa adicional
        }

        @Test
        @DisplayName("Devolución con daño irreparable: cobra reposición, herramienta dada de baja")
        void returnTool_damaged_irreparable() {
            // Given: Un préstamo activo con herramienta de valor 50000
            Long loanId = 103L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2);
            tool.setReplacementValue(50000); // valor de reposición

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(LocalDate.now().minusDays(2));
            activeLoan.setDueDate(LocalDate.now().plusDays(5));
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve con daño irreparable (damaged=true, irreparable=true)
            LoanEntity returned = loanService.returnTool(loanId, true, true);

            // Then:
            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getFine()).isEqualTo(50000.0); // cobra el valor de reposición
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
            assertThat(tool.getStock()).isEqualTo(2); // NO aumenta el stock, se da de baja
        }

        @Test
        @DisplayName("Devolución con atraso Y daño irreparable: multa por atraso + valor de reposición")
        void returnTool_late_and_irreparable() {
            // Given: Préstamo vencido hace 5 días, herramienta vale 80000
            Long loanId = 104L;
            LocalDate dueDate = LocalDate.now().minusDays(5);

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 1);
            tool.setReplacementValue(80000);

            LoanEntity overdueLoan = new LoanEntity();
            overdueLoan.setId(loanId);
            overdueLoan.setClient(client);
            overdueLoan.setTool(tool);
            overdueLoan.setStartDate(LocalDate.now().minusDays(12));
            overdueLoan.setDueDate(dueDate);
            overdueLoan.setReturnDate(null);
            overdueLoan.setStatus("Atrasado");
            overdueLoan.setFine(0.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(overdueLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve con 5 días de atraso Y daño irreparable
            LoanEntity returned = loanService.returnTool(loanId, true, true);

            // Then: Multa = 5 días + 80000 de reposición = 80005.0
            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getStatus()).isEqualTo("Atrasado"); // porque llegó tarde
            assertThat(returned.getFine()).isEqualTo(80005.0); // 5 de atraso + 80000 de reposición
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
            assertThat(tool.getStock()).isEqualTo(1); // no aumenta
        }

        @Test
        @DisplayName("Devolución de préstamo ya devuelto: no hace nada, retorna el mismo préstamo")
        void returnTool_alreadyReturned_noChanges() {
            // Given: Un préstamo que ya fue devuelto anteriormente
            Long loanId = 105L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Disponible", 5);

            LoanEntity alreadyReturned = new LoanEntity();
            alreadyReturned.setId(loanId);
            alreadyReturned.setClient(client);
            alreadyReturned.setTool(tool);
            alreadyReturned.setStartDate(LocalDate.now().minusDays(10));
            alreadyReturned.setDueDate(LocalDate.now().minusDays(3));
            alreadyReturned.setReturnDate(LocalDate.now().minusDays(2)); // ya tiene fecha de devolución
            alreadyReturned.setStatus("Devuelto"); // ya está devuelto
            alreadyReturned.setFine(0.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(alreadyReturned));

            // When: Intentamos devolverlo nuevamente
            LoanEntity returned = loanService.returnTool(loanId, false, false);

            // Then: No debe cambiar nada, retorna el mismo préstamo
            assertThat(returned).isSameAs(alreadyReturned);
            assertThat(returned.getStatus()).isEqualTo("Devuelto");
            assertThat(tool.getStock()).isEqualTo(5); // no cambió

            // No debe guardar nada porque ya estaba devuelto
            verify(loanRepository, never()).save(any());
            verify(toolRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devolución con ID inexistente: lanza IllegalArgumentException")
        void returnTool_notFound_throwsException() {
            // Given: ID de préstamo que no existe
            Long nonExistentId = 999L;

            when(loanRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then: Debe lanzar excepción
            assertThatThrownBy(() -> loanService.returnTool(nonExistentId, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found");

            verify(loanRepository, never()).save(any());
            verify(toolRepository, never()).save(any());
        }

        @Test
        @DisplayName("Devolución con multa preexistente: suma la nueva multa a la anterior")
        void returnTool_withExistingFine_addsFine() {
            // Given: Préstamo que ya tiene una multa de 10.0 y se devuelve 2 días tarde
            Long loanId = 106L;
            LocalDate dueDate = LocalDate.now().minusDays(2);

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 1);

            LoanEntity loanWithFine = new LoanEntity();
            loanWithFine.setId(loanId);
            loanWithFine.setClient(client);
            loanWithFine.setTool(tool);
            loanWithFine.setStartDate(LocalDate.now().minusDays(8));
            loanWithFine.setDueDate(dueDate);
            loanWithFine.setReturnDate(null);
            loanWithFine.setStatus("Atrasado");
            loanWithFine.setFine(10.0); // ya tiene multa de 10.0

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loanWithFine));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When: Se devuelve 2 días tarde
            LoanEntity returned = loanService.returnTool(loanId, false, false);

            // Then: La multa debe ser 10.0 (anterior) + 2.0 (nuevos días) = 12.0
            assertThat(returned.getFine()).isEqualTo(12.0);
            assertThat(returned.getStatus()).isEqualTo("Atrasado");
        }

        // ===== NUEVOS TESTS PARA CUBRIR CASOS CON FINE NULL =====

        @Test
        @DisplayName("returnTool: debe manejar correctamente cuando fine es null al calcular multa por atraso")
        void returnTool_nullFine_calculatesCorrectly() {
            // Given
            Long loanId = 107L;
            LocalDate dueDate = LocalDate.now().minusDays(5); // 5 días de atraso

            ClientEntity client = client(1L);
            ToolEntity tool = tool(1L, "Prestada", 1);

            LoanEntity loan = new LoanEntity();
            loan.setId(loanId);
            loan.setClient(client);
            loan.setTool(tool);
            loan.setStartDate(LocalDate.now().minusDays(10));
            loan.setDueDate(dueDate);
            loan.setReturnDate(null);
            loan.setStatus("Atrasado");
            loan.setFine(null); // Fine es null

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity returned = loanService.returnTool(loanId, false, false);

            // Then
            assertThat(returned.getFine()).isEqualTo(5.0); // 5 días * 1.0
            assertThat(returned.getStatus()).isEqualTo("Atrasado");
        }

        @Test
        @DisplayName("returnTool: debe manejar correctamente daño irreparable con fine null")
        void returnTool_irreparableDamage_nullFine_chargesReplacement() {
            // Given
            Long loanId = 108L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(1L, "Prestada", 1);
            tool.setReplacementValue(50000);

            LoanEntity loan = new LoanEntity();
            loan.setId(loanId);
            loan.setClient(client);
            loan.setTool(tool);
            loan.setStartDate(LocalDate.now().minusDays(3));
            loan.setDueDate(LocalDate.now().plusDays(4));
            loan.setReturnDate(null);
            loan.setStatus("Vigente");
            loan.setFine(null); // Fine es null

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity returned = loanService.returnTool(loanId, true, true);

            // Then
            assertThat(returned.getFine()).isEqualTo(50000.0);
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
        }
    }

    //metodo getAllLoans para completar la cobertura
    @Test
    @DisplayName("getAllLoans: retorna todos los préstamos")
    void getAllLoans_returnsAll() {
        // Given: Varios préstamos en la base de datos
        List<LoanEntity> loans = List.of(
                activeLoan(1L, 10L, LocalDate.now().minusDays(2), LocalDate.now().plusDays(5)),
                activeLoan(2L, 11L, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3))
        );

        when(loanRepository.findAll()).thenReturn(loans);

        // When: Obtenemos todos los préstamos
        List<LoanEntity> result = loanService.getAllLoans();

        // Then: Debe retornar la lista completa
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(loans);

        verify(loanRepository).findAll();
    }

    @Test
    @DisplayName("getAllLoans: debe retornar lista vacía cuando no hay préstamos")
    void getAllLoans_emptyList() {
        // Given
        when(loanRepository.findAll()).thenReturn(List.of());

        // When
        List<LoanEntity> result = loanService.getAllLoans();

        // Then
        assertThat(result).isEmpty();
        verify(loanRepository).findAll();
    }
}