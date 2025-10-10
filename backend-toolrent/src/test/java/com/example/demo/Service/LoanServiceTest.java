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
    @Mock private ConfigService configService;
    @Mock private KardexService kardexService; // ✅ AGREGAR MOCK

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
        l.setRentalCost(0.0);
        l.setDamaged(false);
        l.setIrreparable(false);
        return l;
    }

    // ===========================================================
    // ✅ TESTS DE CÁLCULO DE rentalCost
    // ===========================================================

    @Nested
    @DisplayName("Tests de cálculo de rentalCost")
    class RentalCostTests {

        @Test
        @DisplayName("createLoan: debe calcular rentalCost correctamente (5 días × $7000 = $35000)")
        void createLoan_calculatesRentalCost_5days() {
            Long clientId = 1L, toolId = 10L;
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(5);

            ClientEntity c = client(clientId);
            ToolEntity t = tool(toolId, "Disponible", 3);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(c));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(t));
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0, LoanEntity.class);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            assertThat(loan.getRentalCost()).isEqualTo(7000.0);
            assertThat(loan.getClient().getId()).isEqualTo(clientId);
            assertThat(loan.getTool().getId()).isEqualTo(toolId);
            assertThat(loan.getStatus()).containsIgnoringCase("Vigente");

            verify(configService).getTarifaArriendoDiaria();
            verify(loanRepository).save(any(LoanEntity.class));
            verify(kardexService).registerMovement(any(), any(), any(), any(), any(), any()); // ✅ Verificar llamada
        }

        @Test
        @DisplayName("createLoan: debe calcular rentalCost con tarifa diferente (10 días × $5000 = $50000)")
        void createLoan_calculatesRentalCost_differentRate() {
            Long clientId = 1L, toolId = 10L;
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(10);

            ClientEntity c = client(clientId);
            ToolEntity t = tool(toolId, "Disponible", 2);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(c));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(t));
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0, LoanEntity.class);
                saved.setId(101L);
                return saved;
            });
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            assertThat(loan.getRentalCost()).isEqualTo(5000.0);
            verify(configService).getTarifaArriendoDiaria();
            verify(kardexService).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("createLoan: debe usar mínimo 1 día cuando dueDate es hoy o antes")
        void createLoan_minimumOneDayRental() {
            Long clientId = 1L, toolId = 10L;
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today;

            ClientEntity c = client(clientId);
            ToolEntity t = tool(toolId, "Disponible", 2);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(c));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(t));
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0, LoanEntity.class);
                saved.setId(102L);
                return saved;
            });
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            assertThat(loan.getRentalCost()).isEqualTo(7000.0);
            verify(configService).getTarifaArriendoDiaria();
            verify(kardexService).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
        }
    }

    // ===========================================================
    // TESTS EXISTENTES DE REGLAS DE NEGOCIO
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
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
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
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("Regla: rechaza si el cliente tiene préstamos vencidos (sin returnDate)")
    void rejectWhenClientHasOverdues() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 2)));

        List<LoanEntity> all = new ArrayList<>();
        all.add(activeLoan(clientId, 99L, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)));
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("vencidos");

        verify(loanRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("Regla: máximo 5 préstamos activos por cliente")
    void rejectWhenClientHas5Actives() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 3)));

        List<LoanEntity> all = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            all.add(activeLoan(clientId, (long) i, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3)));
        }
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Máximo 5 préstamos");

        verify(loanRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("Regla: no permitir misma herramienta activa en paralelo para el cliente")
    void rejectDuplicateToolForClient() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(5);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool(toolId, "Disponible", 2)));

        List<LoanEntity> all = List.of(
                activeLoan(clientId, toolId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))
        );
        when(loanRepository.findAll()).thenReturn(all);

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, due))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("misma");

        verify(loanRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("Caso feliz: crea préstamo, calcula rentalCost, descuenta stock y guarda")
    void createLoanHappyPath() {
        Long clientId = 1L, toolId = 10L;
        LocalDate due = LocalDate.now().plusDays(7);

        ClientEntity c = client(clientId);
        ToolEntity t = tool(toolId, "Disponible", 3);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(c));
        when(toolRepository.findById(toolId)).thenReturn(Optional.of(t));
        when(loanRepository.findAll()).thenReturn(List.of());
        when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
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
        assertThat(loan.getRentalCost()).isEqualTo(7000.0);

        assertThat(t.getStock()).isEqualTo(2);

        verify(configService).getTarifaArriendoDiaria();
        verify(loanRepository).save(any(LoanEntity.class));
        verify(toolRepository).save(any(ToolEntity.class));
        verify(kardexService).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("createLoan: debe lanzar 404 cuando cliente no existe")
    void createLoan_clientNotFound_throwsException() {
        Long clientId = 999L;
        Long toolId = 1L;
        LocalDate dueDate = LocalDate.now().plusDays(7);

        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cliente no encontrado")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(clientRepository).findById(clientId);
        verify(toolRepository, never()).findById(any());
        verify(loanRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("createLoan: debe lanzar 404 cuando herramienta no existe")
    void createLoan_toolNotFound_throwsException() {
        Long clientId = 1L;
        Long toolId = 999L;
        LocalDate dueDate = LocalDate.now().plusDays(7);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client(clientId)));
        when(toolRepository.findById(toolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Herramienta no encontrada")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(clientRepository).findById(clientId);
        verify(toolRepository).findById(toolId);
        verify(loanRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando clientId es null")
    void createLoan_nullClientId_throwsBadRequest() {
        assertThatThrownBy(() -> loanService.createLoan(null, 1L, LocalDate.now().plusDays(7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando toolId es null")
    void createLoan_nullToolId_throwsBadRequest() {
        assertThatThrownBy(() -> loanService.createLoan(1L, null, LocalDate.now().plusDays(7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("createLoan: debe lanzar 400 cuando dueDate es null")
    void createLoan_nullDueDate_throwsBadRequest() {
        assertThatThrownBy(() -> loanService.createLoan(1L, 1L, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("clientId, toolId y dueDate son obligatorios")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(clientRepository, never()).findById(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    // ===========================================================
    // ✅ TESTS COMPLETOS DE returnTool
    // ===========================================================

    @Nested
    @DisplayName("Tests de returnTool (devolución de herramientas)")
    class ReturnToolTests {

        @Test
        @DisplayName("Devolución a tiempo en buen estado: sin multa, stock aumenta")
        void returnTool_onTime_noDamage() {
            Long loanId = 100L;
            LocalDate startDate = LocalDate.now().minusDays(5);
            LocalDate dueDate = LocalDate.now().plusDays(2);

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(startDate);
            activeLoan.setDueDate(dueDate);
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);
            activeLoan.setRentalCost(35000.0);
            activeLoan.setDamaged(false);
            activeLoan.setIrreparable(false);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, false, false);

            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getFine()).isEqualTo(0.0);
            assertThat(returned.getStatus()).isEqualTo("Devuelto");
            assertThat(tool.getStock()).isEqualTo(3);
            assertThat(tool.getStatus()).isEqualTo("Disponible");

            verify(loanRepository).save(any(LoanEntity.class));
            verify(toolRepository).save(any(ToolEntity.class));
            verify(kardexService).registerMovement(any(), eq("DEVOLUCION"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("Devolución atrasada: debe calcular multa usando ConfigService")
        void returnTool_late_calculatesFine() {
            Long loanId = 101L;
            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate dueDate = LocalDate.now().minusDays(3);

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(startDate);
            activeLoan.setDueDate(dueDate);
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);
            activeLoan.setRentalCost(70000.0);
            activeLoan.setDamaged(false);
            activeLoan.setIrreparable(false);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, false, false);

            assertThat(returned.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(returned.getFine()).isEqualTo(15000.0);
            assertThat(returned.getStatus()).isEqualTo("Atrasado");

            verify(configService).getTarifaMultaDiaria();
            verify(loanRepository).save(any(LoanEntity.class));
            verify(kardexService).registerMovement(any(), eq("DEVOLUCION"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("✅ NUEVO: returnTool con multa preexistente (fine != null) debe sumar correctamente")
        void returnTool_late_withExistingFine_sumsCorrectly() {
            Long loanId = 105L;
            LocalDate dueDate = LocalDate.now().minusDays(2);

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(LocalDate.now().minusDays(7));
            activeLoan.setDueDate(dueDate);
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(10000.0);
            activeLoan.setRentalCost(49000.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, false, false);

            assertThat(returned.getFine()).isEqualTo(20000.0);
            assertThat(returned.getStatus()).isEqualTo("Atrasado");

            verify(configService).getTarifaMultaDiaria();
            verify(kardexService).registerMovement(any(), eq("DEVOLUCION"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("✅ NUEVO: returnTool con daño reparable debe marcar 'En reparación'")
        void returnTool_damaged_repairable_setsEnReparacion() {
            Long loanId = 106L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 2);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(LocalDate.now().minusDays(3));
            activeLoan.setDueDate(LocalDate.now().plusDays(2));
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(0.0);
            activeLoan.setRentalCost(21000.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, true, false);

            assertThat(returned.getStatus()).isEqualTo("Devuelto");
            assertThat(tool.getStatus()).isEqualTo("En reparación");
            assertThat(tool.getStock()).isEqualTo(2);
            assertThat(returned.getFine()).isEqualTo(0.0);

            verify(toolRepository).save(tool);
            verify(kardexService).registerMovement(any(), eq("REPARACION"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("✅ NUEVO: returnTool con daño irreparable y fine preexistente suma reposición")
        void returnTool_irreparable_withExistingFine_addsReplacement() {
            Long loanId = 107L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Prestada", 1);
            tool.setReplacementValue(50000);

            LoanEntity activeLoan = new LoanEntity();
            activeLoan.setId(loanId);
            activeLoan.setClient(client);
            activeLoan.setTool(tool);
            activeLoan.setStartDate(LocalDate.now().minusDays(5));
            activeLoan.setDueDate(LocalDate.now().minusDays(2));
            activeLoan.setReturnDate(null);
            activeLoan.setStatus("Vigente");
            activeLoan.setFine(8000.0);
            activeLoan.setRentalCost(35000.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(activeLoan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, true, true);

            assertThat(returned.getFine()).isEqualTo(68000.0);
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
            assertThat(returned.getStatus()).isEqualTo("Atrasado");

            verify(configService).getTarifaMultaDiaria();
            verify(toolRepository).save(tool);
            verify(kardexService).registerMovement(any(), eq("BAJA"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("returnTool: debe manejar correctamente daño irreparable con fine null")
        void returnTool_irreparableDamage_nullFine_chargesReplacement() {
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
            loan.setFine(null);
            loan.setRentalCost(49000.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            LoanEntity returned = loanService.returnTool(loanId, true, true);

            assertThat(returned.getFine()).isEqualTo(50000.0);
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
            verify(kardexService).registerMovement(any(), eq("BAJA"), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("✅ NUEVO: returnTool ya devuelto debe retornar sin cambios (early return)")
        void returnTool_alreadyReturned_returnsUnchanged() {
            Long loanId = 109L;

            ClientEntity client = client(1L);
            ToolEntity tool = tool(10L, "Disponible", 5);

            LoanEntity returnedLoan = new LoanEntity();
            returnedLoan.setId(loanId);
            returnedLoan.setClient(client);
            returnedLoan.setTool(tool);
            returnedLoan.setStartDate(LocalDate.now().minusDays(10));
            returnedLoan.setDueDate(LocalDate.now().minusDays(3));
            returnedLoan.setReturnDate(LocalDate.now().minusDays(2));
            returnedLoan.setStatus("Devuelto");
            returnedLoan.setFine(15000.0);
            returnedLoan.setRentalCost(70000.0);

            when(loanRepository.findById(loanId)).thenReturn(Optional.of(returnedLoan));

            LoanEntity result = loanService.returnTool(loanId, false, false);

            assertThat(result.getStatus()).isEqualTo("Devuelto");
            assertThat(result.getFine()).isEqualTo(15000.0);
            assertThat(result.getReturnDate()).isEqualTo(LocalDate.now().minusDays(2));

            verify(loanRepository, never()).save(any());
            verify(toolRepository, never()).save(any());
            verify(configService, never()).getTarifaMultaDiaria();
            verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
        }

        @Test
        @DisplayName("returnTool: debe lanzar IllegalArgumentException cuando préstamo no existe")
        void returnTool_loanNotFound_throwsException() {
            Long loanId = 999L;

            when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.returnTool(loanId, false, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found: " + loanId);

            verify(loanRepository).findById(loanId);
            verify(loanRepository, never()).save(any());
            verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
        }
    }

    // ===========================================================
    // TESTS DE getAllLoans
    // ===========================================================

    @Test
    @DisplayName("getAllLoans: retorna todos los préstamos")
    void getAllLoans_returnsAll() {
        List<LoanEntity> loans = List.of(
                activeLoan(1L, 10L, LocalDate.now().minusDays(2), LocalDate.now().plusDays(5)),
                activeLoan(2L, 11L, LocalDate.now().minusDays(1), LocalDate.now().plusDays(3))
        );

        when(loanRepository.findAll()).thenReturn(loans);

        List<LoanEntity> result = loanService.getAllLoans();

        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(loans);

        verify(loanRepository).findAll();
    }

    @Test
    @DisplayName("getAllLoans: debe retornar lista vacía cuando no hay préstamos")
    void getAllLoans_emptyList() {
        when(loanRepository.findAll()).thenReturn(List.of());

        List<LoanEntity> result = loanService.getAllLoans();

        assertThat(result).isEmpty();
        verify(loanRepository).findAll();
    }
}