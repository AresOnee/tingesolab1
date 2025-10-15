package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ClientController
 *
 * IMPORTANTE: Los datos de prueba deben cumplir con las validaciones definidas en ClientEntity:
 * - RUT: Formato XX.XXX.XXX-X (ejemplo: 12.345.678-9)
 * - Email: Formato válido (ejemplo: usuario@dominio.cl)
 * - Teléfono: Formato +56XXXXXXXXX (ejemplo: +56912345678)
 * - Estado: "Activo" o "Restringido"
 */
@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    @SuppressWarnings("removal")
    private ClientService clientService;

    // ==================== GET ALL CLIENTS ====================

    @Test
    @DisplayName("GET /api/v1/clients/ => 200 y lista de clientes")
    void getAll_ok() throws Exception {
        // Given: Cliente con datos válidos
        ClientEntity c = new ClientEntity(
                1L,
                "Juan Pérez",
                "12.345.678-9",      // ✅ RUT válido
                "+56912345678",      // ✅ Teléfono válido
                "juan@toolrent.cl",  // ✅ Email válido
                "Activo"             // ✅ Estado válido
        );
        when(clientService.getAll()).thenReturn(List.of(c));

        // When & Then
        mvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Juan Pérez"))
                .andExpect(jsonPath("$[0].rut").value("12.345.678-9"))
                .andExpect(jsonPath("$[0].phone").value("+56912345678"))
                .andExpect(jsonPath("$[0].email").value("juan@toolrent.cl"))
                .andExpect(jsonPath("$[0].state").value("Activo"));
    }

    @Test
    @DisplayName("GET /api/v1/clients/ => 200 y lista vacía cuando no hay clientes")
    void getAll_emptyList() throws Exception {
        // Given
        when(clientService.getAll()).thenReturn(List.of());

        // When & Then
        mvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==================== POST (CREATE) CLIENT ====================

    @Test
    @DisplayName("POST /api/v1/clients => 201 y Location header con datos válidos")
    void post_ok() throws Exception {
        // Given: Cliente con todos los campos válidos
        ClientEntity saved = new ClientEntity(
                99L,
                "Diego Manríquez",
                "20.123.456-7",      // ✅ RUT válido
                "+56987654321",      // ✅ Teléfono válido (9 dígitos después de +56)
                "diego@toolrent.cl",
                "Activo"
        );
        when(clientService.create(any(ClientEntity.class))).thenReturn(saved);

        // When & Then
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Diego Manríquez",
                                "rut": "20.123.456-7",
                                "email": "diego@toolrent.cl",
                                "phone": "+56987654321",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.name").value("Diego Manríquez"))
                .andExpect(jsonPath("$.rut").value("20.123.456-7"));
    }

    @Test
    @DisplayName("POST /api/v1/clients => 409 Conflict cuando RUT ya existe")
    void post_conflict_duplicateRut() throws Exception {
        // Given: Servicio lanza excepción de conflicto
        when(clientService.create(any(ClientEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El RUT ya existe en el sistema"));

        // When & Then
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Diego Test",
                                "rut": "12.345.678-9",
                                "email": "diego@test.cl",
                                "phone": "+56912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 409 Conflict cuando email ya existe")
    void post_conflict_duplicateEmail() throws Exception {
        // Given
        when(clientService.create(any(ClientEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe en el sistema"));

        // When & Then
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Diego Test",
                                "rut": "11.111.111-1",
                                "email": "existente@toolrent.cl",
                                "phone": "+56999999999",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 400 Bad Request con nombre vacío")
    void post_badRequest_emptyName() throws Exception {
        // When & Then: Las validaciones @NotBlank fallan antes de llegar al servicio
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "",
                                "rut": "12.345.678-9",
                                "email": "test@test.cl",
                                "phone": "+56912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 400 Bad Request con RUT inválido")
    void post_badRequest_invalidRut() throws Exception {
        // When & Then: La validación @Pattern del RUT falla
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Test Usuario",
                                "rut": "12345678",
                                "email": "test@test.cl",
                                "phone": "+56912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 400 Bad Request con email inválido")
    void post_badRequest_invalidEmail() throws Exception {
        // When & Then
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Test Usuario",
                                "rut": "12.345.678-9",
                                "email": "email-invalido",
                                "phone": "+56912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 400 Bad Request con teléfono inválido")
    void post_badRequest_invalidPhone() throws Exception {
        // When & Then: Teléfono sin formato +56XXXXXXXXX
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Test Usuario",
                                "rut": "12.345.678-9",
                                "email": "test@test.cl",
                                "phone": "912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clients => 400 Bad Request con estado inválido")
    void post_badRequest_invalidState() throws Exception {
        // When & Then: Estado que no es "Activo" ni "Restringido"
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Test Usuario",
                                "rut": "12.345.678-9",
                                "email": "test@test.cl",
                                "phone": "+56912345678",
                                "state": "Inactivo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    // ==================== PUT (UPDATE) CLIENT ====================

    @Test
    @DisplayName("PUT /api/v1/clients/{id} => 200 con datos actualizados")
    void put_ok() throws Exception {
        // Given
        ClientEntity updated = new ClientEntity(
                1L,
                "Juan Pérez Actualizado",
                "12.345.678-9",
                "+56999999999",
                "juan.nuevo@toolrent.cl",
                "Activo"
        );
        when(clientService.update(any(Long.class), any(ClientEntity.class))).thenReturn(updated);

        // When & Then
        mvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Juan Pérez Actualizado",
                                "rut": "12.345.678-9",
                                "email": "juan.nuevo@toolrent.cl",
                                "phone": "+56999999999",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Juan Pérez Actualizado"))
                .andExpect(jsonPath("$.email").value("juan.nuevo@toolrent.cl"));
    }

    @Test
    @DisplayName("PUT /api/v1/clients/{id} => 404 cuando cliente no existe")
    void put_notFound() throws Exception {
        // Given
        when(clientService.update(any(Long.class), any(ClientEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        // When & Then
        mvc.perform(put("/api/v1/clients/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Test",
                                "rut": "12.345.678-9",
                                "email": "test@test.cl",
                                "phone": "+56912345678",
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    // ==================== PATCH (UPDATE STATE) CLIENT ====================

    @Test
    @DisplayName("PATCH /api/v1/clients/{id}/state => 200 al cambiar estado")
    void patchState_ok() throws Exception {
        // Given
        ClientEntity updated = new ClientEntity(
                1L,
                "Juan Pérez",
                "12.345.678-9",
                "+56912345678",
                "juan@toolrent.cl",
                "Restringido"  // Estado cambiado
        );
        when(clientService.updateState(any(Long.class), any(String.class))).thenReturn(updated);

        // When & Then
        mvc.perform(patch("/api/v1/clients/1/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "state": "Restringido"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Restringido"));
    }

    @Test
    @DisplayName("PATCH /api/v1/clients/{id}/state => 400 con estado inválido")
    void patchState_badRequest() throws Exception {
        // Given
        when(clientService.updateState(any(Long.class), any(String.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Estado inválido. Debe ser 'Activo' o 'Restringido'"));

        // When & Then
        mvc.perform(patch("/api/v1/clients/1/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "state": "Inactivo"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/clients/{id}/state => 404 cuando cliente no existe")
    void patchState_notFound() throws Exception {
        // Given
        when(clientService.updateState(any(Long.class), any(String.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        // When & Then
        mvc.perform(patch("/api/v1/clients/999/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "state": "Activo"
                            }
                            """))
                .andExpect(status().isNotFound());
    }
}