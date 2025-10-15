package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
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
 * Tests completos para ClientService (Gap Analysis - Puntos 8 y 9)
 *
 * ✅ Tests de CRUD completo
 * ✅ Tests de validaciones de formato (delegadas a Bean Validation)
 * ✅ Tests de reglas de negocio
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService - Tests Completos (CRUD + Validaciones)")
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
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
            client.setId(null); // Nuevo cliente sin ID

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
            assertThat(saved.getRut()).isEqualTo("12.345.678-9");
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
                    .hasMessageContaining("email ya existe")
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
    @DisplayName("Tests de UPDATE (RF3.1 - Punto 8)")
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
            assertThat(updated.getRut()).isEqualTo("12.345.678-9"); // RUT no cambia
            verify(clientRepository).save(existing);
        }

        @Test
        @DisplayName("✅ update debe permitir mantener el mismo email")
        void update_withSameEmail_shouldSucceed() {
            // Given
            Long clientId = 1L;
            ClientEntity existing = validClient();
            existing.setId(clientId);

            ClientEntity updates = new ClientEntity();
            updates.setName("Juan Actualizado");
            updates.setPhone("+56987654321");
            updates.setEmail("juan@toolrent.cl"); // Mismo email

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existing));
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ClientEntity updated = clientService.update(clientId, updates);

            // Then
            assertThat(updated.getName()).isEqualTo("Juan Actualizado");
            verify(clientRepository, never()).existsByEmail(any()); // No verifica porque es el mismo
        }

        @Test
        @DisplayName("❌ update debe rechazar si el nuevo email ya existe")
        void update_withDuplicateEmail_shouldThrow409() {
            // Given
            Long clientId = 1L;
            ClientEntity existing = validClient();

            ClientEntity updates = new ClientEntity();
            updates.setName("Juan Actualizado");
            updates.setPhone("+56987654321");
            updates.setEmail("otro@toolrent.cl"); // Email diferente que ya existe

            when(clientRepository.findById(clientId)).thenReturn(Optional.of(existing));
            when(clientRepository.existsByEmail("otro@toolrent.cl")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> clientService.update(clientId, updates))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("email ya existe")
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ update debe lanzar 404 si el cliente no existe")
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
    @DisplayName("Tests de UPDATE STATE (RF3.2 - Punto 8)")
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
}