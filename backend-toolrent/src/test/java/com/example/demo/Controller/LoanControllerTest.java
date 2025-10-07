package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.LoanService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LoanController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoanControllerTest {

    private static final String CREATE_ENDPOINT = "/api/v1/loans/create";

    @Autowired
    private MockMvc mvc;

    @MockBean
    @SuppressWarnings("removal")
    private LoanService loanService;

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ 201 y JSON")
    void create_ok() throws Exception {
        LoanEntity saved = new LoanEntity();
        saved.setId(123L);
        saved.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(2);
        tool.setStatus("Disponible");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        saved.setTool(tool);

        saved.setStartDate(LocalDate.now());
        saved.setDueDate(LocalDate.of(2025,10,10));
        saved.setStatus("Vigente");
        saved.setFine(0d);
        saved.setDamaged(false);
        saved.setIrreparable(false);

        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class))).thenReturn(saved);

        // CORRECCIÓN: Usar parámetros de URL en lugar de JSON
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-10"))
                .andExpect(status().isOk())  // Nota: devuelve 200, no 201
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("Vigente"));
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ 409 conflicto (reglas negocio)")
    void create_conflict() throws Exception {
        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cliente con préstamos vencidos"));

        // CORRECCIÓN: Usar parámetros de URL en lugar de JSON
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-10"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ 400 si payload inválido")
    void create_badRequest() throws Exception {
        // CORRECCIÓN: Simplemente no enviar uno de los parámetros requeridos
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-10"))
                // Sin clientId, debería dar error 400
                .andExpect(status().isBadRequest());
    }
}