package com.example.demo.Controller;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.ToolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ToolController
 *
 * ✅ CORREGIDO: Agregado any(String.class) para username en todos los mocks
 */
@WebMvcTest(ToolController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ToolController - Tests de Endpoints")
class ToolControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ToolService toolService;

    // Helper para crear herramientas de prueba
    private ToolEntity tool(Long id, String name, String status, Integer stock) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName(name);
        t.setCategory("Eléctricas");
        t.setStatus(status);
        t.setStock(stock);
        t.setReplacementValue(50000);
        return t;
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("GET /api/v1/tools/ => 200 y lista de herramientas")
    void getAllTools_ok() throws Exception {
        // Given
        ToolEntity t1 = tool(1L, "Taladro", "Disponible", 5);
        ToolEntity t2 = tool(2L, "Martillo", "Disponible", 10);

        when(toolService.getAllTools()).thenReturn(List.of(t1, t2));

        // When & Then
        mvc.perform(get("/api/v1/tools/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Taladro"))
                .andExpect(jsonPath("$[1].name").value("Martillo"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("POST /api/v1/tools/ => 201 cuando se crea exitosamente")
    void create_ok() throws Exception {
        // Given
        ToolEntity tool = tool(10L, "Sierra Circular", "Disponible", 3);

        // ✅ CORREGIDO: Agregado any(String.class) para username
        when(toolService.create(any(ToolEntity.class), any(String.class)))
                .thenReturn(tool);

        String requestBody = """
                {
                    "name": "Sierra Circular",
                    "category": "Eléctricas",
                    "status": "Disponible",
                    "stock": 3,
                    "replacementValue": 50000
                }
                """;

        // When & Then
        mvc.perform(post("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Sierra Circular"))
                .andExpect(jsonPath("$.stock").value(3));

        // ✅ Verificar que se llamó con username
        verify(toolService).create(any(ToolEntity.class), eq("admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("PUT /api/v1/tools/{id}/decommission => 200 cuando se da de baja exitosamente")
    void decommission_ok() throws Exception {
        // Given
        Long toolId = 5L;
        ToolEntity decommissioned = tool(toolId, "Taladro Dañado", "Dada de baja", 0);

        // ✅ CORREGIDO: Agregado any(String.class) para username
        when(toolService.decommission(eq(toolId), any(String.class)))
                .thenReturn(decommissioned);

        // When & Then
        mvc.perform(put("/api/v1/tools/{id}/decommission", toolId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Dada de baja"))
                .andExpect(jsonPath("$.stock").value(0));

        // ✅ Verificar que se llamó con username
        verify(toolService).decommission(toolId, "admin");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("POST /api/v1/tools/ => 403 cuando usuario no es admin")
    void create_forbidden() throws Exception {
        String requestBody = """
                {
                    "name": "Martillo",
                    "category": "Manuales",
                    "status": "Disponible",
                    "stock": 10,
                    "replacementValue": 15000
                }
                """;

        // When & Then
        mvc.perform(post("/api/v1/tools/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("PUT /api/v1/tools/{id}/decommission => 403 cuando usuario no es admin")
    void decommission_forbidden() throws Exception {
        // When & Then
        mvc.perform(put("/api/v1/tools/5/decommission"))
                .andExpect(status().isForbidden());
    }
}