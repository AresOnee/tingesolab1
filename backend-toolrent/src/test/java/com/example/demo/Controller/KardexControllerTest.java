package com.example.demo.Controller;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.KardexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KardexController.class)
@DisplayName("KardexController Tests")
class KardexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KardexService kardexService;

    private KardexEntity kardex;
    private ToolEntity tool;

    @BeforeEach
    void setUp() {
        tool = new ToolEntity();
        tool.setId(1L);
        tool.setName("Taladro");
        tool.setCategory("Electricas");
        tool.setReplacementValue(50000);
        tool.setStock(5);
        tool.setStatus("Disponible");

        kardex = new KardexEntity();
        kardex.setId(1L);
        kardex.setMovementType("REGISTRO");
        kardex.setTool(tool);
        kardex.setQuantity(5);
        kardex.setUsername("ADMIN");
        kardex.setMovementDate(LocalDateTime.now());
        kardex.setObservations("Test observation");
        kardex.setLoanId(null);
    }

    // ===== TESTS PARA GET /api/v1/kardex/tool/{toolId} =====

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/tool/{toolId} debe retornar movimientos de la herramienta")
    void getMovementsByTool_withUser_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByTool(1L)).thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/tool/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].movementType").value("REGISTRO"))
                .andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].username").value("ADMIN"))
                .andExpect(jsonPath("$[0].observations").value("Test observation"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex/tool/{toolId} debe funcionar con rol ADMIN")
    void getMovementsByTool_withAdmin_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByTool(1L)).thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/tool/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("REGISTRO"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/tool/{toolId} debe retornar lista vacía si no hay movimientos")
    void getMovementsByTool_noMovements_returnsEmptyList() throws Exception {
        // Given
        when(kardexService.getMovementsByTool(1L)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/tool/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/tool/{toolId} debe retornar 404 si herramienta no existe")
    void getMovementsByTool_toolNotFound_returns404() throws Exception {
        // Given
        when(kardexService.getMovementsByTool(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Herramienta no encontrada"));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/tool/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/kardex/tool/{toolId} sin autenticación debe retornar 401")
    void getMovementsByTool_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/kardex/tool/1"))
                .andExpect(status().isUnauthorized());
    }

    // ===== TESTS PARA GET /api/v1/kardex/date-range =====

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex/date-range debe filtrar por fechas")
    void getMovementsByDateRange_success_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("REGISTRO"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/date-range debe funcionar con rol USER")
    void getMovementsByDateRange_withUser_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex/date-range debe retornar lista vacía si no hay movimientos en el rango")
    void getMovementsByDateRange_noMovementsInRange_returnsEmptyList() throws Exception {
        // Given
        when(kardexService.getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/date-range")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex/date-range debe retornar 400 si fechas son inválidas")
    void getMovementsByDateRange_invalidDates_returns400() throws Exception {
        // Given
        when(kardexService.getMovementsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fechas inválidas"));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/date-range")
                        .param("startDate", "2025-12-31")
                        .param("endDate", "2025-01-01")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ===== TESTS PARA GET /api/v1/kardex =====

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex debe retornar todos los movimientos")
    void getAllMovements_success_returns200() throws Exception {
        // Given
        KardexEntity kardex2 = new KardexEntity();
        kardex2.setId(2L);
        kardex2.setMovementType("PRESTAMO");
        kardex2.setTool(tool);
        kardex2.setQuantity(-1);
        kardex2.setUsername("USER");
        kardex2.setMovementDate(LocalDateTime.now());

        when(kardexService.getAllMovements()).thenReturn(List.of(kardex, kardex2));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].movementType").value("REGISTRO"))
                .andExpect(jsonPath("$[1].movementType").value("PRESTAMO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex debe retornar lista vacía si no hay movimientos")
    void getAllMovements_noMovements_returnsEmptyList() throws Exception {
        // Given
        when(kardexService.getAllMovements()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/kardex").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/kardex sin autenticación debe retornar 401")
    void getAllMovements_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/kardex"))
                .andExpect(status().isUnauthorized());
    }

    // ===== TESTS PARA GET /api/v1/kardex/type/{movementType} =====

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/type/{movementType} debe filtrar por tipo")
    void getMovementsByType_success_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByType("PRESTAMO")).thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/type/PRESTAMO").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("REGISTRO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/kardex/type/{movementType} debe aceptar tipo en minúsculas")
    void getMovementsByType_lowercaseType_returns200() throws Exception {
        // Given
        when(kardexService.getMovementsByType("prestamo")).thenReturn(List.of(kardex));

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/type/prestamo").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/type/{movementType} debe retornar lista vacía si no hay movimientos del tipo")
    void getMovementsByType_noMovementsOfType_returnsEmptyList() throws Exception {
        // Given
        when(kardexService.getMovementsByType("BAJA")).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/kardex/type/BAJA").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/kardex/type/{movementType} debe aceptar todos los tipos válidos")
    void getMovementsByType_allValidTypes_return200() throws Exception {
        // Given
        String[] validTypes = {"REGISTRO", "PRESTAMO", "DEVOLUCION", "BAJA", "REPARACION"};

        for (String type : validTypes) {
            when(kardexService.getMovementsByType(type)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/v1/kardex/type/" + type).with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("GET /api/v1/kardex/type/{movementType} sin autenticación debe retornar 401")
    void getMovementsByType_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/kardex/type/PRESTAMO"))
                .andExpect(status().isUnauthorized());
    }
}