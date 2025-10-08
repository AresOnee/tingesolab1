package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock private ClientRepository clientRepository;
    @InjectMocks private ClientService clientService;

    private ClientEntity client(String rut, String email) {
        ClientEntity c = new ClientEntity();
        c.setName("Nombre");
        c.setRut(rut);
        c.setEmail(email);
        c.setPhone("+569...");
        c.setState("Activo");
        return c;
    }

    private ClientEntity client(String rut, String email, String state) {
        ClientEntity c = new ClientEntity();
        c.setName("Nombre");
        c.setRut(rut);
        c.setEmail(email);
        c.setPhone("+569...");
        c.setState(state);
        return c;
    }

    @Test
    @DisplayName("getAll delega en repository y retorna lista")
    void getAll_returnsList() {
        when(clientRepository.findAll()).thenReturn(List.of(client("1-9", "a@a.cl")));

        List<ClientEntity> result = clientService.getAll();

        assertThat(result).hasSize(1);
        verify(clientRepository).findAll();
    }

    @Test
    @DisplayName("create OK cuando RUT y email son únicos")
    void create_ok() {
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity saved = inv.getArgument(0, ClientEntity.class);
            saved.setId(100L);
            return saved;
        });

        ClientEntity saved = clientService.create(body);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRut()).isEqualTo("12.345.678-9");
        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("create lanza 409 si RUT ya existe")
    void create_conflict_rut() {
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(body))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("RUT ya existe");

        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("create lanza 409 si email ya existe")
    void create_conflict_email() {
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(body))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("email ya existe");

        verify(clientRepository, never()).save(any());
    }

    // ========== NUEVOS TESTS PARA CUBRIR BRANCHES FALTANTES ==========

    @Test
    @DisplayName("create: debe setear estado 'Activo' cuando state es null")
    void create_setsActivoWhenStateIsNull() {
        // Given
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl", null);

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity saved = inv.getArgument(0, ClientEntity.class);
            saved.setId(100L);
            return saved;
        });

        // When
        ClientEntity saved = clientService.create(body);

        // Then
        assertThat(saved.getState()).isEqualTo("Activo");
        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("create: debe setear estado 'Activo' cuando state es blank")
    void create_setsActivoWhenStateIsBlank() {
        // Given
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl", "   ");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity saved = inv.getArgument(0, ClientEntity.class);
            saved.setId(100L);
            return saved;
        });

        // When
        ClientEntity saved = clientService.create(body);

        // Then
        assertThat(saved.getState()).isEqualTo("Activo");
        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("create: debe mantener estado cuando tiene valor válido")
    void create_keepsValidState() {
        // Given
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl", "Restringido");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity saved = inv.getArgument(0, ClientEntity.class);
            saved.setId(100L);
            return saved;
        });

        // When
        ClientEntity saved = clientService.create(body);

        // Then
        assertThat(saved.getState()).isEqualTo("Restringido");
        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("create: debe validar RUT único antes de email")
    void create_validatesRutBeforeEmail() {
        // Given
        ClientEntity body = client("12.345.678-9", "juan@toolrent.cl", "Activo");
        when(clientRepository.existsByRut(body.getRut())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> clientService.create(body))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("RUT")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(clientRepository).existsByRut(body.getRut());
        verify(clientRepository, never()).existsByEmail(any());
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAll: debe retornar lista vacía cuando no hay clientes")
    void getAll_emptyList() {
        // Given
        when(clientRepository.findAll()).thenReturn(List.of());

        // When
        List<ClientEntity> result = clientService.getAll();

        // Then
        assertThat(result).isEmpty();
        verify(clientRepository).findAll();
    }

    @Test
    @DisplayName("getAll: debe retornar todos los clientes")
    void getAll_returnsAllClients() {
        // Given
        ClientEntity c1 = client("12.345.678-9", "juan@toolrent.cl", "Activo");
        ClientEntity c2 = client("98.765.432-1", "maria@toolrent.cl", "Restringido");
        when(clientRepository.findAll()).thenReturn(List.of(c1, c2));

        // When
        List<ClientEntity> result = clientService.getAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(c1, c2);
        verify(clientRepository).findAll();
    }

    @Test
    @DisplayName("create: debe crear cliente con todos los campos válidos")
    void create_withAllValidFields() {
        // Given
        ClientEntity body = new ClientEntity();
        body.setName("Diego Pérez");
        body.setRut("20.204.010-5");
        body.setEmail("diego@toolrent.cl");
        body.setPhone("+56912345678");
        body.setState("Activo");

        when(clientRepository.existsByRut(body.getRut())).thenReturn(false);
        when(clientRepository.existsByEmail(body.getEmail())).thenReturn(false);
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(inv -> {
            ClientEntity saved = inv.getArgument(0, ClientEntity.class);
            saved.setId(99L);
            return saved;
        });

        // When
        ClientEntity saved = clientService.create(body);

        // Then
        assertThat(saved.getId()).isEqualTo(99L);
        assertThat(saved.getName()).isEqualTo("Diego Pérez");
        assertThat(saved.getRut()).isEqualTo("20.204.010-5");
        assertThat(saved.getEmail()).isEqualTo("diego@toolrent.cl");
        assertThat(saved.getState()).isEqualTo("Activo");
        verify(clientRepository).save(any(ClientEntity.class));
    }
}