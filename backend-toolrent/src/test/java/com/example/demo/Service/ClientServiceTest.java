package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ✅ TESTS COMPLETOS PARA CLIENTSERVICE - VERSIÓN COMPLETA CON 90%+ COBERTURA
 *
 * ✅ INCLUYE:
 * 1. Tests CRUD básicos
 * 2. Tests de métodos legacy (getAllClients, getClientById, saveClient)
 * 3. Tests de RF3.2 (updateClientStateBasedOnLoans, updateAllClientStates)
 * 4. Tests de scheduledUpdateClientStates con manejo de excepciones
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService - Tests Completos para 90%+ Cobertura")
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @Mock private LoanRepository loanRepository;
    @InjectMocks private ClientService clientService;

    // ==================== HELPERS ====================

    private ClientEntity validClient() {
        ClientEntity c = new ClientEntity();
        c.setId(1L);
        c.setName("Juan Pérez");
        c.setRut("12.345.678-9");
        c.setEmail("juan@toolrent.cl");
        c.setPhone("+56912345678");
        c.setState("Activo");
        return c;
    }

    private ClientEntity validClientWithState(String state) {
        ClientEntity c = validClient();
        c.setState(state);
        return c;
    }

    // ==================== TESTS DE CREATE ====================

    @Nested
    @DisplayName("Tests de CREATE (RF3.1)")
    class CreateTests {

        @Test
        @DisplayName("✅ Debe crear cliente con todos los campos válidos")
        void create_withValidFields_shouldSucceed() {
            // Given
            ClientEntity client = validClient();
            client.setId(null);

            when(clientRepository.existsByRut(client.getRut())).thenReturn(false);
            when(clientRepository.existsByEmail(client.getEmail())).thenReturn(false);
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
                ClientEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // When
            ClientEntity saved = clientService.create(client);

            // Then
            assertThat(saved.getId()).isEqualTo(100L);
            assertThat(saved.getName()).isEqualTo("Juan Pérez");
            verify(clientRepository).save(any(ClientEntity.class));
        }

        @Test
        @DisplayName("✅ Debe asignar estado 'Activo' por defecto cuando state es null")
        void create_withNullState_shouldSetActivo() {
            // Given
            ClientEntity client = validClient();
            client.setState(null);

            when(clientRepository.existsByRut(client.getRut())).thenReturn(false);
            when(clientRepository.existsByEmail(client.getEmail())).thenReturn(false);
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
                ClientEntity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // When
            ClientEntity saved = clientService.create(client);

            // Then
            assertThat(saved.getState()).isEqualTo("Activo");
        }

        @Test
        @DisplayName("❌ Debe rechazar si RUT ya existe")
        void create_withDuplicateRut_shouldThrow409() {
            // Given
            ClientEntity client = validClient();
            when(clientRepository.existsByRut(client.getRut())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("RUT ya existe")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Debe rechazar si email ya existe")
        void create_withDuplicateEmail_shouldThrow409() {
            // Given
            ClientEntity client = validClient();
            when(clientRepository.existsByRut(client.getRut())).thenReturn(false);
            when(clientRepository.existsByEmail(client.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clientService.create(client))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email ya existe")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(clientRepository, never()).save(any());
        }
    }

    // ==================== TESTS DE READ ====================

    @Nested
    @DisplayName("Tests de READ (RF3.1)")
    class ReadTests {

        @Test
        @DisplayName("✅ getAll debe retornar todos los clientes")
        void getAll_shouldReturnAllClients() {
            // Given
            ClientEntity c1 = validClient();
            ClientEntity c2 = validClientWithState("Restringido");
            c2.setId(2L);
            c2.setRut("98.765.432-1");
            c2.setEmail("maria@toolrent.cl");

            when(clientRepository.findAll()).thenReturn(List.of(c1, c2));

            // When
            List<ClientEntity> result = clientService.getAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(c1, c2);
        }

        @Test
        @DisplayName("✅ getById debe retornar cliente existente")
        void getById_withExistingId_shouldReturnClient() {
            // Given
            ClientEntity client = validClient();
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            // When
            ClientEntity found = clientService.getById(1L);

            // Then
            assertThat(found).isEqualTo(client);
            assertThat(found.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("❌ getById debe lanzar 404 si no existe")
        void getById_withNonExistentId_shouldThrow404() {
            // Given
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clientService.getById(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== TESTS DE UPDATE ====================

    @Nested
    @DisplayName("Tests de UPDATE (RF3.1)")
    class UpdateTests {

        @Test
        @DisplayName("✅ update debe actualizar name, phone y email")
        void update_withValidData_shouldUpdateFields() {
            // Given
            Long clientId = 1L;
            ClientEntity existing = validClient();
            existing.setId(clientId);

            ClientEntity updates = new ClientEntity();
            updates.setName("Juan Carlos Pérez");
            updates.setPhone("+56987654321");
            updates.setEmail("juancarlos@toolrent.cl");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existing));
            when(clientRepository.existsByEmail(updates.getEmail())).thenReturn(false);
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ClientEntity updated = clientService.update(clientId, updates);

            // Then
            assertThat(updated.getName()).isEqualTo("Juan Carlos Pérez");
            assertThat(updated.getPhone()).isEqualTo("+56987654321");
            assertThat(updated.getEmail()).isEqualTo("juancarlos@toolrent.cl");
            verify(clientRepository).save(existing);
        }

        @Test
        @DisplayName("❌ update debe rechazar si email ya existe")
        void update_withDuplicateEmail_shouldThrow409() {
            // Given
            Long clientId = 1L;
            ClientEntity existing = validClient();
            existing.setId(clientId);

            ClientEntity updates = new ClientEntity();
            updates.setEmail("otro@toolrent.cl");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existing));
            when(clientRepository.existsByEmail(updates.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clientService.update(clientId, updates))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email ya existe")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ update debe lanzar 404 si cliente no existe")
        void update_withNonExistentId_shouldThrow404() {
            // Given
            Long clientId = 999L;
            ClientEntity updates = validClient();
            when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clientService.update(clientId, updates))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== TESTS DE UPDATE STATE ====================

    @Nested
    @DisplayName("Tests de UPDATE STATE (RF3.2)")
    class UpdateStateTests {

        @Test
        @DisplayName("✅ updateState debe cambiar de Activo a Restringido")
        void updateState_fromActivoToRestringido_shouldSucceed() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Activo");
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ClientEntity updated = clientService.updateState(clientId, "Restringido");

            // Then
            assertThat(updated.getState()).isEqualTo("Restringido");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("✅ updateState debe cambiar de Restringido a Activo")
        void updateState_fromRestringidoToActivo_shouldSucceed() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Restringido");
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ClientEntity updated = clientService.updateState(clientId, "Activo");

            // Then
            assertThat(updated.getState()).isEqualTo("Activo");
        }

        @Test
        @DisplayName("❌ updateState debe rechazar estado inválido")
        void updateState_withInvalidState_shouldThrow400() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClient();
            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

            // When & Then
            assertThatThrownBy(() -> clientService.updateState(clientId, "Inválido"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Estado inválido")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ updateState debe lanzar 404 si cliente no existe")
        void updateState_withNonExistentClient_shouldThrow404() {
            // Given
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clientService.updateState(999L, "Activo"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ==================== TESTS DE DELETE ====================

    @Nested
    @DisplayName("Tests de DELETE (CRUD completo)")
    class DeleteTests {

        @Test
        @DisplayName("✅ delete debe eliminar cliente existente")
        void delete_withExistingId_shouldSucceed() {
            // Given
            Long clientId = 1L;
            when(clientRepository.existsById(clientId)).thenReturn(true);
            doNothing().when(clientRepository).deleteById(clientId);

            // When
            clientService.delete(clientId);

            // Then
            verify(clientRepository).deleteById(clientId);
        }

        @Test
        @DisplayName("❌ delete debe lanzar 404 si cliente no existe")
        void delete_withNonExistentId_shouldThrow404() {
            // Given
            Long clientId = 999L;
            when(clientRepository.existsById(clientId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clientService.delete(clientId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("no encontrado")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(clientRepository, never()).deleteById(any());
        }
    }

    // ==================== TESTS DE MÉTODOS LEGACY ====================

    @Nested
    @DisplayName("Tests de Métodos Legacy (Compatibilidad Controller)")
    class LegacyMethodsTests {

        @Test
        @DisplayName("✅ getAllClients debe retornar lista de clientes")
        void getAllClients_shouldReturnList() {
            // Given
            ClientEntity c1 = validClient();
            ClientEntity c2 = validClientWithState("Restringido");
            c2.setId(2L);

            when(clientRepository.findAll()).thenReturn(List.of(c1, c2));

            // When
            List<ClientEntity> result = clientService.getAllClients();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(c1, c2);
        }

        @Test
        @DisplayName("✅ getClientById debe retornar cliente si existe")
        void getClientById_found_shouldReturnClient() {
            // Given
            ClientEntity client = validClient();
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            // When
            ClientEntity result = clientService.getClientById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("✅ getClientById debe retornar null si no existe")
        void getClientById_notFound_shouldReturnNull() {
            // Given
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            ClientEntity result = clientService.getClientById(999L);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("✅ saveClient debe guardar y retornar cliente")
        void saveClient_shouldSaveAndReturn() {
            // Given
            ClientEntity client = validClient();
            when(clientRepository.save(client)).thenReturn(client);

            // When
            ClientEntity saved = clientService.saveClient(client);

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isEqualTo(1L);
            verify(clientRepository).save(client);
        }
    }

    // ==================== TESTS DE RF3.2: ACTUALIZACIÓN AUTOMÁTICA ====================

    @Nested
    @DisplayName("Tests de RF3.2: updateClientStateBasedOnLoans")
    class UpdateClientStateBasedOnLoansTests {

        @Test
        @DisplayName("✅ Debe cambiar cliente Activo a Restringido si tiene problemas")
        void updateClientStateBasedOnLoans_activoToRestringido() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Activo");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(true);
            doNothing().when(clientRepository).updateClientState(clientId, "Restringido");

            // When
            boolean result = clientService.updateClientStateBasedOnLoans(clientId);

            // Then
            assertThat(result).isTrue();
            verify(clientRepository).updateClientState(clientId, "Restringido");
        }

        @Test
        @DisplayName("✅ Debe cambiar cliente Restringido a Activo si no tiene problemas")
        void updateClientStateBasedOnLoans_restringidoToActivo() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Restringido");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);
            doNothing().when(clientRepository).updateClientState(clientId, "Activo");

            // When
            boolean result = clientService.updateClientStateBasedOnLoans(clientId);

            // Then
            assertThat(result).isTrue();
            verify(clientRepository).updateClientState(clientId, "Activo");
        }

        @Test
        @DisplayName("✅ No debe cambiar si cliente Activo sin problemas")
        void updateClientStateBasedOnLoans_noChange_activoSinProblemas() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Activo");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(false);

            // When
            boolean result = clientService.updateClientStateBasedOnLoans(clientId);

            // Then
            assertThat(result).isFalse();
            verify(clientRepository, never()).updateClientState(anyLong(), anyString());
        }

        @Test
        @DisplayName("✅ No debe cambiar si cliente Restringido con problemas")
        void updateClientStateBasedOnLoans_noChange_restringidoConProblemas() {
            // Given
            Long clientId = 1L;
            ClientEntity client = validClientWithState("Restringido");

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
            when(loanRepository.hasOverduesOrFines(clientId)).thenReturn(true);

            // When
            boolean result = clientService.updateClientStateBasedOnLoans(clientId);

            // Then
            assertThat(result).isFalse();
            verify(clientRepository, never()).updateClientState(anyLong(), anyString());
        }

        @Test
        @DisplayName("✅ Debe retornar false si cliente no existe")
        void updateClientStateBasedOnLoans_clientNotFound() {
            // Given
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            boolean result = clientService.updateClientStateBasedOnLoans(999L);

            // Then
            assertThat(result).isFalse();
            verify(loanRepository, never()).hasOverduesOrFines(anyLong());
        }
    }

    @Nested
    @DisplayName("Tests de RF3.2: updateAllClientStates")
    class UpdateAllClientStatesTests {

        @Test
        @DisplayName("✅ Debe actualizar todos los clientes con problemas")
        void updateAllClientStates_shouldUpdateAllClients() {
            // Given
            ClientEntity c1 = validClientWithState("Activo");
            c1.setId(1L);
            ClientEntity c2 = validClientWithState("Restringido");
            c2.setId(2L);
            ClientEntity c3 = validClientWithState("Activo");
            c3.setId(3L);

            when(clientRepository.findAll()).thenReturn(List.of(c1, c2, c3));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(true);  // c1: Activo -> Restringido
            when(loanRepository.hasOverduesOrFines(2L)).thenReturn(false); // c2: Restringido -> Activo
            when(loanRepository.hasOverduesOrFines(3L)).thenReturn(false); // c3: Activo (sin cambio)

            // When
            clientService.updateAllClientStates();

            // Then
            verify(clientRepository).updateClientState(1L, "Restringido");
            verify(clientRepository).updateClientState(2L, "Activo");
            verify(clientRepository, never()).updateClientState(eq(3L), anyString());
        }

        @Test
        @DisplayName("✅ Debe manejar lista vacía sin errores")
        void updateAllClientStates_emptyList() {
            // Given
            when(clientRepository.findAll()).thenReturn(List.of());

            // When
            clientService.updateAllClientStates();

            // Then
            verify(loanRepository, never()).hasOverduesOrFines(anyLong());
            verify(clientRepository, never()).updateClientState(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Tests de scheduledUpdateClientStates (Tarea Programada)")
    class ScheduledUpdateClientStatesTests {

        @Test
        @DisplayName("✅ Debe ejecutar actualización automática exitosamente")
        void scheduledUpdateClientStates_success() {
            // Given
            ClientEntity c1 = validClientWithState("Activo");
            c1.setId(1L);

            when(clientRepository.findAll()).thenReturn(List.of(c1));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);

            // When
            clientService.scheduledUpdateClientStates();

            // Then
            verify(clientRepository).findAll();
        }

        @Test
        @DisplayName("✅ Debe manejar excepciones durante actualización automática")
        void scheduledUpdateClientStates_withException() {
            // Given
            when(clientRepository.findAll()).thenThrow(new RuntimeException("Database error"));

            // When - No debe lanzar excepción, solo imprimir error
            assertThatCode(() -> clientService.scheduledUpdateClientStates())
                    .doesNotThrowAnyException();

            // Then
            verify(clientRepository).findAll();
        }
    }
}