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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests COMPLETOS para LoanService
 *
 * Cobertura completa de:
 * - createLoan() con todas las validaciones
 * - returnTool() con todos los escenarios
 * - returnTool() con cargo por reparación (Épica 2 RN #16) ✅ NUEVO
 * - getAllLoans()
 *
 * ✅ CORREGIDO: Todos los métodos ahora usan 4 parámetros (agregado username)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService - Tests Completos para 100% Cobertura")
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ConfigService configService;
    @Mock private KardexService kardexService;

    @InjectMocks private LoanService loanService;

    // ==================== HELPER METHODS ====================

    private ClientEntity client(Long id, String name, String state) {
        ClientEntity c = new ClientEntity();
        c.setId(id);
        c.setName(name);
        c.setRut("12.345.678-" + id);
        c.setEmail("client" + id + "@toolrent.cl");
        c.setPhone("+56912345678");
        c.setState(state);
        return c;
    }

    private ToolEntity tool(Long id, String name, String status, Integer stock, Integer replacementValue) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory("Categoria");
        t.setStatus(status);
        t.setStock(stock);
        t.setReplacementValue(replacementValue);
        return t;
    }

    private LoanEntity loan(Long id, ClientEntity client, ToolEntity tool, LocalDate startDate, LocalDate dueDate, String status) {
        LoanEntity l = new LoanEntity();
        l.setId(id);
        l.setClient(client);
        l.setTool(tool);
        l.setStartDate(startDate);
        l.setDueDate(dueDate);
        l.setReturnDate(null);
        l.setStatus(status);
        l.setFine(0.0);
        l.setRentalCost(7000.0);
        l.setDamaged(false);
        l.setIrreparable(false);
        return l;
    }

    // ==================== TESTS DE createLoan() ====================

    @Nested
    @DisplayName("createLoan() - Validaciones de parámetros")
    class CreateLoanParameterValidationTests {

        @Test
        @DisplayName("Debe lanzar excepción si clientId es null")
        void createLoan_nullClientId_throwsException() {
            assertThatThrownBy(() -> loanService.createLoan(null, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("obligatorios")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Debe lanzar excepción si toolId es null")
        void createLoan_nullToolId_throwsException() {
            assertThatThrownBy(() -> loanService.createLoan(1L, null, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("obligatorios")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Debe lanzar excepción si dueDate es null")
        void createLoan_nullDueDate_throwsException() {
            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, null, "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("obligatorios")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Debe lanzar excepción si cliente no existe")
        void createLoan_clientNotFound_throwsException() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Cliente no encontrado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Debe lanzar excepción si herramienta no existe")
        void createLoan_toolNotFound_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Herramienta no encontrada")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("createLoan() - Reglas de negocio")
    class CreateLoanBusinessRulesTests {

        @Test
        @DisplayName("Debe rechazar si cliente está Restringido")
        void createLoan_clientRestricted_throwsException() {
            ClientEntity client = client(1L, "Juan", "Restringido");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Restringido")
                    .hasMessageContaining("Solo clientes activos");
        }

        @Test
        @DisplayName("Debe rechazar si cliente tiene multas pendientes")
        void createLoan_clientWithFines_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(true);

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("multas pendientes");
        }

        @Test
        @DisplayName("Debe rechazar si dueDate es anterior a hoy")
        void createLoan_dueDateInPast_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);

            LocalDate yesterday = LocalDate.now().minusDays(1);

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, yesterday, "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("fecha de devolución no puede ser anterior");
        }

        @Test
        @DisplayName("Debe rechazar si herramienta no está Disponible")
        void createLoan_toolNotAvailable_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no está disponible");
        }

        @Test
        @DisplayName("Debe rechazar si herramienta no tiene stock")
        void createLoan_toolNoStock_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 0, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("sin stock");
        }

        @Test
        @DisplayName("Debe rechazar si cliente tiene préstamos vencidos")
        void createLoan_clientWithOverdueLoans_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            // Préstamo vencido del cliente
            LoanEntity overdueLoan = loan(100L, client, tool,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(3),
                    "Vigente");

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of(overdueLoan));

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("préstamos vencidos");
        }

        @Test
        @DisplayName("Debe rechazar si cliente tiene 5 préstamos activos")
        void createLoan_clientWithMaxActiveLoans_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            // 5 préstamos activos
            List<LoanEntity> activeLoans = List.of(
                    loan(101L, client, tool(20L, "T1", "Prestada", 0, 10000),
                            LocalDate.now(), LocalDate.now().plusDays(5), "Vigente"),
                    loan(102L, client, tool(21L, "T2", "Prestada", 0, 10000),
                            LocalDate.now(), LocalDate.now().plusDays(5), "Vigente"),
                    loan(103L, client, tool(22L, "T3", "Prestada", 0, 10000),
                            LocalDate.now(), LocalDate.now().plusDays(5), "Vigente"),
                    loan(104L, client, tool(23L, "T4", "Prestada", 0, 10000),
                            LocalDate.now(), LocalDate.now().plusDays(5), "Vigente"),
                    loan(105L, client, tool(24L, "T5", "Prestada", 0, 10000),
                            LocalDate.now(), LocalDate.now().plusDays(5), "Vigente")
            );

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(activeLoans);

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Máximo 5 préstamos activos");
        }

        @Test
        @DisplayName("Debe rechazar si cliente ya tiene préstamo activo de la misma herramienta")
        void createLoan_clientWithSameToolActive_throwsException() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            // Préstamo activo de la misma herramienta
            LoanEntity existingLoan = loan(100L, client, tool,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(5),
                    "Vigente");

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of(existingLoan));

            assertThatThrownBy(() -> loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ya tiene un préstamo activo de la misma herramienta");
        }
    }

    @Nested
    @DisplayName("createLoan() - Casos exitosos")
    class CreateLoanSuccessTests {

        @Test
        @DisplayName("Debe crear préstamo exitosamente con cálculo correcto de rentalCost")
        void createLoan_success_calculatesRentalCost() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity result = loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getClient().getId()).isEqualTo(1L);
            assertThat(result.getTool().getId()).isEqualTo(10L);
            assertThat(result.getStatus()).isEqualTo("Vigente");
            assertThat(result.getRentalCost()).isEqualTo(49000.0); // 7 días × 7000
            assertThat(result.getFine()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Debe calcular rentalCost mínimo de 1 día cuando dueDate = hoy")
        void createLoan_sameDayDueDate_minimumOneDayCharge() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocalDate today = LocalDate.now();

            // When
            LoanEntity result = loanService.createLoan(1L, 10L, today, "testuser");

            // Then: Mínimo 1 día
            assertThat(result.getRentalCost()).isEqualTo(7000.0);
        }

        @Test
        @DisplayName("Debe reducir stock de herramienta al crear préstamo")
        void createLoan_reducesToolStock() {
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "testuser");

            // Then: Stock debe reducirse de 5 a 4
            assertThat(tool.getStock()).isEqualTo(4);
            verify(toolRepository).save(tool);
        }
    }

    // ==================== TESTS DE returnTool() ====================

    @Nested
    @DisplayName("returnTool() - Devolución sin daños")
    class ReturnToolNormalTests {

        @Test
        @DisplayName("Debe procesar devolución a tiempo sin daños")
        void returnTool_onTime_noDamage_success() {
            // Given: Préstamo vigente, devolución a tiempo
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2), // Vence en 2 días
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity result = loanService.returnTool(100L, false, false, "testuser");

            // Then
            assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
            assertThat(result.getStatus()).isEqualTo("Devuelto");
            assertThat(result.getFine()).isEqualTo(0.0);
            assertThat(result.getDamaged()).isFalse();
            assertThat(tool.getStock()).isEqualTo(5); // 4 + 1

            verify(kardexService).registerMovement(10L, "DEVOLUCION", 1, "testuser",
                    "Devolución normal (loan #100)", 100L);
        }

        @Test
        @DisplayName("Debe calcular multa cuando devolución está atrasada")
        void returnTool_late_calculatesFine() {
            // Given: Préstamo vencido hace 3 días
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(3), // Venció hace 3 días
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity result = loanService.returnTool(100L, false, false, "testuser");

            // Then
            assertThat(result.getStatus()).isEqualTo("Atrasado");
            assertThat(result.getFine()).isEqualTo(15000.0); // 3 días × 5000
            assertThat(tool.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("Debe retornar préstamo ya devuelto sin cambios")
        void returnTool_alreadyReturned_noChanges() {
            // Given: Préstamo ya devuelto
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Disponible", 5, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(2),
                    "Devuelto");
            loan.setReturnDate(LocalDate.now().minusDays(2));

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

            // When
            LoanEntity result = loanService.returnTool(100L, false, false, "testuser");

            // Then: No debe hacer cambios
            assertThat(result.getStatus()).isEqualTo("Devuelto");
            verify(toolRepository, never()).save(any());
            verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("returnTool() - Devolución con daños")
    class ReturnToolDamagedTests {

        @Test
        @DisplayName("Debe procesar devolución con daño reparable")
        void returnTool_repairableDamage_success() {
            // Given: Devolución con daño reparable
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Dañado = true, Irreparable = false
            LoanEntity result = loanService.returnTool(100L, true, false, "testuser");

            // Then
            assertThat(result.getDamaged()).isTrue();
            assertThat(result.getIrreparable()).isFalse();
            assertThat(tool.getStatus()).isEqualTo("En reparación");
            assertThat(tool.getStock()).isEqualTo(5); // Se incrementa

            verify(kardexService).registerMovement(10L, "DEVOLUCION", 1, "testuser",
                    "Devolución con daño reparable (loan #100)", 100L);
        }

        @Test
        @DisplayName("Debe procesar devolución con daño irreparable (baja)")
        void returnTool_irreparableDamage_toolWrittenOff() {
            // Given: Devolución con daño irreparable
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Dañado = true, Irreparable = true
            LoanEntity result = loanService.returnTool(100L, true, true, "testuser");

            // Then
            assertThat(result.getDamaged()).isTrue();
            assertThat(result.getIrreparable()).isTrue();
            assertThat(result.getFine()).isEqualTo(50000.0); // Valor de reposición
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");
            assertThat(tool.getStock()).isEqualTo(4); // NO se incrementa

            verify(kardexService).registerMovement(10L, "BAJA", 0, "testuser",
                    "Baja por daño irreparable (loan #100)", 100L);
        }

        @Test
        @DisplayName("Debe sumar multa de reposición a multa por atraso")
        void returnTool_irreparableDamage_withLateFine() {
            // Given: Préstamo atrasado con daño irreparable
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(2), // Atrasado 2 días
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity result = loanService.returnTool(100L, true, true, "testuser");

            // Then: Multa = (2 días × 5000) + 50000 = 60000
            assertThat(result.getFine()).isEqualTo(60000.0);
            assertThat(result.getStatus()).isEqualTo("Atrasado");
        }
    }

    // ==================== ✅ NUEVOS TESTS: CARGO POR REPARACIÓN ====================

    @Nested
    @DisplayName("returnTool() - Cargo por Reparación (Épica 2 RN #16)")
    class ReturnToolRepairChargeTests {

        @Test
        @DisplayName("✅ Debe aplicar cargo por reparación cuando hay daño leve")
        void returnTool_withRepairableDamage_shouldApplyRepairCharge() {
            // Given: Préstamo con devolución a tiempo pero con daño reparable
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2), // No hay atraso
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(configService.getCargoReparacion()).thenReturn(10000.0); // ✅ CARGO CONFIGURABLE
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Devolver con daño reparable
            LoanEntity result = loanService.returnTool(100L, true, false, "testuser");

            // Then: Debe aplicar cargo de reparación
            assertThat(result.getFine()).isEqualTo(10000.0);
            assertThat(tool.getStatus()).isEqualTo("En reparación");

            verify(configService).getCargoReparacion(); // ✅ Debe llamarse
        }

        @Test
        @DisplayName("✅ Cargo reparación NO debe aplicarse si no hay daños")
        void returnTool_noDamage_noRepairCharge() {
            // Given: Devolución normal sin daños
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Devolver sin daños
            LoanEntity result = loanService.returnTool(100L, false, false, "testuser");

            // Then: NO debe aplicar cargo de reparación
            assertThat(result.getFine()).isEqualTo(0.0);
            assertThat(tool.getStatus()).isEqualTo("Disponible");

            verify(configService, never()).getCargoReparacion(); // ✅ No debe llamarse
        }

        @Test
        @DisplayName("✅ Cargo reparación NO debe aplicarse si daño es irreparable")
        void returnTool_irreparableDamage_noRepairCharge() {
            // Given: Devolución con daño irreparable
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Devolver con daño irreparable
            LoanEntity result = loanService.returnTool(100L, true, true, "testuser");

            // Then: Debe cobrar valor de reposición, NO cargo de reparación
            assertThat(result.getFine()).isEqualTo(50000.0); // Valor de reposición
            assertThat(tool.getStatus()).isEqualTo("Dada de baja");

            verify(configService, never()).getCargoReparacion(); // ✅ No debe llamarse
        }

        @Test
        @DisplayName("✅ Debe respetar cargo configurable de $0 (reparación gratuita)")
        void returnTool_freeRepair_whenChargeIsZero() {
            // Given: Cargo de reparación configurado en $0
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool = tool(10L, "Taladro", "Prestada", 4, 50000);

            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(configService.getCargoReparacion()).thenReturn(0.0); // ✅ Reparación gratuita
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Devolver con daño reparable
            LoanEntity result = loanService.returnTool(100L, true, false, "testuser");

            // Then: No debe cobrar nada
            assertThat(result.getFine()).isEqualTo(0.0);
            assertThat(tool.getStatus()).isEqualTo("En reparación");

            verify(configService).getCargoReparacion();
        }

        @Test
        @DisplayName("✅ Cada préstamo debe aplicar su propio cargo de reparación")
        void returnTool_independentRepairCharges_multipleLoans() {
            // Given: 2 préstamos diferentes con daños reparables
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool1 = tool(10L, "Taladro", "Prestada", 4, 50000);
            ToolEntity tool2 = tool(11L, "Martillo", "Prestada", 2, 30000);

            LoanEntity loan1 = loan(100L, client, tool1,
                    LocalDate.now().minusDays(3),
                    LocalDate.now().plusDays(2),
                    "Vigente");

            LoanEntity loan2 = loan(200L, client, tool2,
                    LocalDate.now().minusDays(5),
                    LocalDate.now().plusDays(1),
                    "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan1));
            when(loanRepository.findById(200L)).thenReturn(Optional.of(loan2));
            when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);
            when(configService.getCargoReparacion())
                    .thenReturn(10000.0)  // Primera llamada
                    .thenReturn(15000.0); // Segunda llamada (cargo diferente)
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Devolver ambos préstamos con daños
            LoanEntity result1 = loanService.returnTool(100L, true, false, "testuser");
            when(configService.getCargoReparacion()).thenReturn(15000.0);

            LoanEntity result2 = loanService.returnTool(200L, true, false, "testuser");

            // Then: Cada préstamo debe tener su cargo respectivo
            assertThat(result1.getFine()).isEqualTo(10000.0);
            assertThat(result2.getFine()).isEqualTo(15000.0);

            verify(configService, times(2)).getCargoReparacion();
        }
    }

    // ==================== TESTS DE CASOS EXCEPCIONALES ====================

    @Nested
    @DisplayName("returnTool() - Casos excepcionales")
    class ReturnToolExceptionTests {

        @Test
        @DisplayName("Debe lanzar excepción si préstamo no existe")
        void returnTool_loanNotFound_throwsException() {
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.returnTool(999L, false, false, "testuser"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Loan not found: 999");
        }
    }

    // ==================== TESTS DE getAllLoans() ====================

    @Nested
    @DisplayName("getAllLoans()")
    class GetAllLoansTests {

        @Test
        @DisplayName("Debe retornar lista de todos los préstamos")
        void getAllLoans_returnsAllLoans() {
            // Given
            ClientEntity client = client(1L, "Juan", "Activo");
            ToolEntity tool1 = tool(10L, "Taladro", "Prestada", 4, 50000);
            ToolEntity tool2 = tool(11L, "Martillo", "Prestada", 3, 15000);

            LoanEntity loan1 = loan(100L, client, tool1,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(5),
                    "Vigente");
            LoanEntity loan2 = loan(101L, client, tool2,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(3),
                    "Vigente");

            when(loanRepository.findAll()).thenReturn(List.of(loan1, loan2));

            // When
            List<LoanEntity> result = loanService.getAllLoans();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(loan1, loan2);
            verify(loanRepository).findAll();
        }

        @Test
        @DisplayName("Debe retornar lista vacía cuando no hay préstamos")
        void getAllLoans_returnsEmptyList() {
            when(loanRepository.findAll()).thenReturn(List.of());

            List<LoanEntity> result = loanService.getAllLoans();

            assertThat(result).isEmpty();
            verify(loanRepository).findAll();
        }
    }
}