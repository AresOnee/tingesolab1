package com.example.demo.Controller;

import com.example.demo.Controller.ToolController;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.ToolService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ToolController.class)
@AutoConfigureMockMvc(addFilters = false)
class ToolControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean @SuppressWarnings("removal")  private ToolService toolService;

    @Test
    @DisplayName("GET /api/v1/tools/ ⇒ 200 y lista")
    void getAll_ok() throws Exception {
        ToolEntity t = new ToolEntity();
        t.setId(1L); t.setName("Taladro"); t.setCategory("Eléctricas"); t.setStock(3); t.setStatus("Disponible"); t.setReplacementValue(120000);
        when(toolService.getAllTools()).thenReturn(List.of(t));

        mvc.perform(get("/api/v1/tools/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Taladro"));
    }

    @Test
    @DisplayName("POST /api/v1/tools ⇒ 201 y cuerpo")
    void post_ok() throws Exception {
        ToolEntity saved = new ToolEntity();
        saved.setId(77L); saved.setName("Rotomartillo"); saved.setCategory("Eléctricas"); saved.setStock(3); saved.setStatus("Disponible"); saved.setReplacementValue(120000);
        when(toolService.create(any(ToolEntity.class))).thenReturn(saved);

        // Tu controller tiene @PostMapping({ "", "/" }) así que /api/v1/tools funciona
        mvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
          {"name":"Rotomartillo","category":"Eléctricas","replacementValue":120000,"stock":3,"status":"Disponible"}
        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77));
    }

    @Test
    @DisplayName("POST /api/v1/tools ⇒ 409 si nombre duplicado")
    void post_conflict() throws Exception {
        when(toolService.create(any(ToolEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Nombre duplicado"));

        mvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"name":"Rotomartillo","category":"Eléctricas","replacementValue":120000,"stock":3,"status":"Disponible"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/tools ⇒ 400 si nombre en blanco")
    void post_badRequest_blankName() throws Exception {
        when(toolService.create(any(ToolEntity.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio"));

        mvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
          {"name":"   ","category":"Eléctricas","replacementValue":120000,"stock":3}
        """))
                .andExpect(status().isBadRequest());
    }

    // ===== TESTS PARA RF1.2: DAR DE BAJA HERRAMIENTAS =====

    @Test
    @DisplayName("PUT /api/v1/tools/{id}/decommission ⇒ 200 con herramienta dada de baja")
    void decommission_ok() throws Exception {
        Long toolId = 1L;
        ToolEntity decommissioned = new ToolEntity();
        decommissioned.setId(toolId);
        decommissioned.setName("Taladro");
        decommissioned.setCategory("Eléctricas");
        decommissioned.setStatus("Dada de baja");
        decommissioned.setStock(0);
        decommissioned.setReplacementValue(120000);

        when(toolService.decommission(toolId)).thenReturn(decommissioned);

        mvc.perform(put("/api/v1/tools/{id}/decommission", toolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(toolId))
                .andExpect(jsonPath("$.status").value("Dada de baja"))
                .andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    @DisplayName("PUT /api/v1/tools/{id}/decommission ⇒ 404 si herramienta no existe")
    void decommission_notFound() throws Exception {
        Long toolId = 999L;
        when(toolService.decommission(toolId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Herramienta no encontrada"));

        mvc.perform(put("/api/v1/tools/{id}/decommission", toolId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/tools/{id}/decommission ⇒ 400 si herramienta está prestada")
    void decommission_toolIsLoaned() throws Exception {
        Long toolId = 1L;
        when(toolService.decommission(toolId))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede dar de baja una herramienta que está prestada"));

        mvc.perform(put("/api/v1/tools/{id}/decommission", toolId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/tools/{id}/decommission ⇒ 400 si herramienta ya está dada de baja")
    void decommission_alreadyDecommissioned() throws Exception {
        Long toolId = 1L;
        when(toolService.decommission(toolId))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La herramienta ya está dada de baja"));

        mvc.perform(put("/api/v1/tools/{id}/decommission", toolId))
                .andExpect(status().isBadRequest());
    }
}