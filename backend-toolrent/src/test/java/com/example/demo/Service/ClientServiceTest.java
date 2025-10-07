package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}

