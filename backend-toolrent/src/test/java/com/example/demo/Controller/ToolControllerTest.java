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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
          {"name":"Rotomartillo","category":"Eléctricas","replacementValue":120000,"stock":3}
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
}

