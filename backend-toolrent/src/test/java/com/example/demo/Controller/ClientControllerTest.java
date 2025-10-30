package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ClientController
 *
 * ✅ CORREGIDO: Usa los métodos correctos del ClientService
 */
@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientController - Tests de Endpoints")
class ClientControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClientService clientService;

    // Helper para crear clientes de prueba
    private ClientEntity client(Long id, String name, String rut, String state) {
        ClientEntity c = new ClientEntity();
        c.setId(id);
        c.setName(name);
        c.setRut(rut);
        c.setEmail(name.toLowerCase().replace(" ", "") + "@toolrent.cl");
        c.setPhone("+56912345678");
        c.setState(state);
        return c;
    }

    // ==================== GET ALL CLIENTS ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("GET /api/v1/clients/ => 200 y lista de clientes")
    void getAll_ok() throws Exception {
        // Given
        ClientEntity c1 = client(1L, "Juan Pérez", "12.345.678-9", "Activo");
        ClientEntity c2 = client(2L, "María González", "98.765.432-1", "Activo");

        when(clientService.getAllClients()).thenReturn(List.of(c1, c2));

        // When & Then
        mvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Juan Pérez"))
                .andExpect(jsonPath("$[1].name").value("María González"));
    }

    // ==================== GET CLIENT BY ID ====================

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("GET /api/v1/clients/{id} => 200 cuando cliente existe")
    void getById_ok() throws Exception {
        // Given
        ClientEntity client = client(5L, "Ana Gómez", "11.111.111-1", "Activo");

        when(clientService.getClientById(5L)).thenReturn(client);

        // When & Then
        mvc.perform(get("/api/v1/clients/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Ana Gómez"))
                .andExpect(jsonPath("$.rut").value("11.111.111-1"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("GET /api/v1/clients/{id} => 404 cuando cliente no existe")
    void getById_notFound() throws Exception {
        // Given
        when(clientService.getClientById(999L)).thenReturn(null);

        // When & Then
        mvc.perform(get("/api/v1/clients/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== CREATE CLIENT ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/clients/ => 200 cuando se crea exitosamente")
    void create_ok() throws Exception {
        // Given
        ClientEntity client = client(10L, "Pedro Soto", "22.222.222-2", "Activo");

        when(clientService.saveClient(any(ClientEntity.class))).thenReturn(client);

        String requestBody = """
                {
                    "name": "Pedro Soto",
                    "rut": "22.222.222-2",
                    "email": "pedro@toolrent.cl",
                    "phone": "+56912345678",
                    "state": "Activo"
                }
                """;

        // When & Then
        mvc.perform(post("/api/v1/clients/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pedro Soto"))
                .andExpect(jsonPath("$.rut").value("22.222.222-2"));

        verify(clientService).saveClient(any(ClientEntity.class));
    }


    // ==================== UPDATE CLIENT ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/clients/{id} => 200 cuando se actualiza exitosamente")
    void update_ok() throws Exception {
        // Given
        Long clientId = 3L;
        ClientEntity existing = client(clientId, "Carlos López", "33.333.333-3", "Activo");
        ClientEntity updated = client(clientId, "Carlos López Actualizado", "33.333.333-3", "Activo");

        when(clientService.getClientById(clientId)).thenReturn(existing);
        when(clientService.saveClient(any(ClientEntity.class))).thenReturn(updated);

        String requestBody = """
                {
                    "name": "Carlos López Actualizado",
                    "phone": "+56987654321"
                }
                """;

        // When & Then
        mvc.perform(put("/api/v1/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carlos López Actualizado"));

        verify(clientService).saveClient(any(ClientEntity.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/clients/{id} => 404 cuando cliente no existe")
    void update_notFound() throws Exception {
        // Given
        when(clientService.getClientById(999L)).thenReturn(null);

        String requestBody = """
                {
                    "name": "Nombre Actualizado"
                }
                """;

        // When & Then
        mvc.perform(put("/api/v1/clients/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/clients/{id} => 400 cuando estado es inválido")
    void update_invalidState() throws Exception {
        // Given
        Long clientId = 3L;
        ClientEntity existing = client(clientId, "Carlos", "33.333.333-3", "Activo");

        when(clientService.getClientById(clientId)).thenReturn(existing);

        String requestBody = """
                {
                    "state": "EstadoInvalido"
                }
                """;

        // When & Then
        mvc.perform(put("/api/v1/clients/{id}", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    // ==================== UPDATE CLIENT STATE ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/clients/{id}/update-state => 200 con estado actualizado")
    void updateClientState_ok() throws Exception {
        // Given
        Long clientId = 5L;
        ClientEntity updated = client(clientId, "Ana", "55.555.555-5", "Restringido");

        when(clientService.updateClientStateBasedOnLoans(clientId)).thenReturn(true);
        when(clientService.getClientById(clientId)).thenReturn(updated);

        // When & Then
        mvc.perform(put("/api/v1/clients/{id}/update-state", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true))
                .andExpect(jsonPath("$.clientId").value(5))
                .andExpect(jsonPath("$.currentState").value("Restringido"));
    }

    // ==================== UPDATE ALL CLIENT STATES ====================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/clients/update-all-states => 200 con mensaje de éxito")
    void updateAllClientStates_ok() throws Exception {
        // Given
        doNothing().when(clientService).updateAllClientStates();

        // When & Then
        mvc.perform(post("/api/v1/clients/update-all-states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(clientService).updateAllClientStates();
    }
}