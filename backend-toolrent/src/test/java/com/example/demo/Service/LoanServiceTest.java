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
import static org.mockito.Mockito.*;

/**
 * Tests para las nuevas validaciones implementadas (Gap Analysis - Punto 4)
 * ✅ Validación de estado del cliente
 * ✅ Validación de multas pendientes
 * ✅ Validación de fechas
 * ✅ Cálculo correcto de rentalCost
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de LoanService - Nuevas Validaciones (Gap Analysis)")
class LoanServiceNewValidationsTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ConfigService configService;
    @Mock private KardexService kardexService;

    @InjectMocks private LoanService loanService;

    // Helper methods
    private ClientEntity client(Long id, String state) {
        ClientEntity c = new ClientEntity();
        c.setId(id);
        c.setName("Cliente " + id);
        c.setRut("11.111.111-" + id);
        c.setEmail("c"+id+"@toolrent.cl");
        c.setPhone("+5690000" + id);
        c.setState(state);
        return c;
    }

    private ToolEntity tool(Long id, String status, Integer stock) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName("Herr " + id);
        t.setCategory("Cat");
        t.setReplacementValue(100000);
        t.setStock(stock);
        t.setStatus(status);
        return t;
    }

    // ========== TESTS DE NUEVA VALIDACIÓN 1: ESTADO DEL CLIENTE ==========

    @Nested
    @DisplayName("Validación de Estado del Cliente")
    class ClientStateValidationTests {

        @Test
        @DisplayName("✅ Debe rechazar préstamo si cliente está en estado 'Restringido'")
        void createLoan_clientRestringido_shouldThrowException() {
            // Given: Cliente en estado "Restringido"
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().plusDays(7);

            ClientEntity restrictedClient = client(clientId, "Restringido");
            ToolEntity availableTool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(restrictedClient));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(availableTool));

            // When & Then: Debe lanzar excepción
            assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Restringido")
                    .hasMessageContaining("Solo clientes activos pueden solicitar préstamos")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            // Verify: No se debe guardar el préstamo
            verify(loanRepository, never()).save(any());
            verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("✅ Debe rechazar préstamo si cliente está en estado 'Inactivo'")
        void createLoan_clientInactivo_shouldThrowException() {
            // Given: Cliente en estado "Inactivo"
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().plusDays(7);

            ClientEntity inactiveClient = client(clientId, "Inactivo");
            ToolEntity availableTool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(inactiveClient));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(availableTool));

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("estado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Debe permitir préstamo si cliente está en estado 'Activo'")
        void createLoan_clientActivo_shouldSucceed() {
            // Given: Cliente ACTIVO, sin multas, sin préstamos vencidos
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().plusDays(7);

            ClientEntity activeClient = client(clientId, "Activo");
            ToolEntity availableTool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(activeClient));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(availableTool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false); // ✅ Sin multas
            when(loanRepository.findAll()).thenReturn(List.of()); // Sin préstamos previos
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When: Crear préstamo
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then: Debe crear el préstamo exitosamente
            assertThat(loan).isNotNull();
            assertThat(loan.getClient().getState()).isEqualTo("Activo");
            verify(loanRepository).save(any(LoanEntity.class));
            verify(kardexService).registerMovement(any(), any(), any(), any(), any(), any());
        }
    }

    // ========== TESTS DE NUEVA VALIDACIÓN 2: MULTAS PENDIENTES ==========

    @Nested
    @DisplayName("Validación de Multas Pendientes")
    class OverduesAndFinesValidationTests {

        @Test
        @DisplayName("✅ Debe rechazar préstamo si cliente tiene multas pendientes")
        void createLoan_clientWithFines_shouldThrowException() {
            // Given: Cliente con multas pendientes
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().plusDays(7);

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(true); // ✅ Tiene multas

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("multas pendientes")
                    .hasMessageContaining("préstamos atrasados")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(loanRepository, never()).save(any());
            verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("✅ Debe permitir préstamo si cliente NO tiene multas pendientes")
        void createLoan_clientWithoutFines_shouldSucceed() {
            // Given: Cliente SIN multas
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().plusDays(7);

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false); // ✅ Sin multas
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then
            assertThat(loan).isNotNull();
            verify(loanRepository).save(any(LoanEntity.class));
        }
    }

    // ========== TESTS DE NUEVA VALIDACIÓN 3: FECHAS ==========

    @Nested
    @DisplayName("Validación de Fechas")
    class DateValidationTests {

        @Test
        @DisplayName("✅ Debe rechazar préstamo si dueDate es anterior a today")
        void createLoan_dueDateInPast_shouldThrowException() {
            // Given: Fecha de devolución en el pasado
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now().minusDays(1); // ❌ Ayer

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> loanService.createLoan(clientId, toolId, dueDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("fecha de devolución no puede ser anterior")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Debe permitir préstamo si dueDate es hoy (mismo día)")
        void createLoan_dueDateToday_shouldSucceed() {
            // Given: Fecha de devolución es HOY
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now(); // ✅ Hoy

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then: Debe permitir (mínimo 1 día)
            assertThat(loan).isNotNull();
            assertThat(loan.getRentalCost()).isEqualTo(7000.0); // 1 día × 7000
            verify(loanRepository).save(any());
        }
    }

    // ========== TESTS DE CORRECCIÓN: CÁLCULO DE rentalCost ==========

    @Nested
    @DisplayName("Cálculo Correcto de rentalCost")
    class RentalCostCalculationTests {

        @Test
        @DisplayName("✅ Debe calcular rentalCost = días × tarifa (5 días × $7000 = $35000)")
        void createLoan_calculatesRentalCostCorrectly_5days() {
            // Given: Préstamo de 5 días
            Long clientId = 1L, toolId = 10L;
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(5); // 5 días

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then: 5 días × $7000 = $35000
            assertThat(loan.getRentalCost()).isEqualTo(35000.0);
            verify(configService).getTarifaArriendoDiaria();
        }

        @Test
        @DisplayName("✅ Debe calcular rentalCost = días × tarifa (10 días × $5000 = $50000)")
        void createLoan_calculatesRentalCostCorrectly_10days() {
            // Given: Préstamo de 10 días con tarifa de $5000
            Long clientId = 1L, toolId = 10L;
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(10);

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then: 10 días × $5000 = $50000
            assertThat(loan.getRentalCost()).isEqualTo(50000.0);
        }

        @Test
        @DisplayName("✅ Debe usar mínimo 1 día cuando dueDate es hoy")
        void createLoan_minimumOneDay_whenSameDay() {
            // Given: Préstamo que vence HOY (0 días de diferencia)
            Long clientId = 1L, toolId = 10L;
            LocalDate dueDate = LocalDate.now();

            ClientEntity client = client(clientId, "Activo");
            ToolEntity tool = tool(toolId, "Disponible", 5);

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(List.of());
            when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);
            when(loanRepository.save(any())).thenAnswer(inv -> {
                LoanEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            LoanEntity loan = loanService.createLoan(clientId, toolId, dueDate);

            // Then: Mínimo 1 día → $7000
            assertThat(loan.getRentalCost()).isEqualTo(7000.0);
        }
    }
}