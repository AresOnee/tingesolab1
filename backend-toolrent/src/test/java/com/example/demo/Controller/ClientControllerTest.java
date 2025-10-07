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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    @SuppressWarnings("removal")
    private ClientService clientService;

    @Test
    @DisplayName("GET /api/v1/clients/ ⇒ 200 y lista")
    void getAll_ok() throws Exception {
        ClientEntity c = new ClientEntity(1L, "Juan", "12.345.678-9", "+569...", "juan@toolrent.cl", "Activo");
        when(clientService.getAll()).thenReturn(List.of(c));

        mvc.perform(get("/api/v1/clients/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Juan"));
    }

    @Test
    @DisplayName("POST /api/v1/clients ⇒ 201 y Location")
    void post_ok() throws Exception {
        ClientEntity saved = new ClientEntity(99L,"Diego","20.204.010-5","+569...","diego@toolrent.cl","Activo");
        when(clientService.create(any(ClientEntity.class))).thenReturn(saved);

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Diego","rut":"20.204.010-5","email":"diego@toolrent.cl","phone":"+569...","state":"Activo"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.name").value("Diego"));
    }

    @Test
    @DisplayName("POST /api/v1/clients ⇒ 409 si RUT/email duplicados")
    void post_conflict() throws Exception {
        when(clientService.create(any(ClientEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "El RUT ya existe"));

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"Diego","rut":"20.204.010-5","email":"diego@toolrent.cl","phone":"+569...","state":"Activo"}
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/clients ⇒ 400 si payload inválido")
    void post_badRequest() throws Exception {
        when(clientService.create(any(ClientEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos inválidos"));

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":""}
                            """))
                .andExpect(status().isBadRequest());
    }
}