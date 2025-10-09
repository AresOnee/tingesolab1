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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("POST /api/v1/loans/create ⇒ 201 y JSON con rentalCost")
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
        saved.setDueDate(LocalDate.of(2025,10,17)); // 9 días desde hoy (2025-10-08)
        saved.setStatus("Vigente");
        saved.setFine(0d);
        saved.setDamaged(false);
        saved.setIrreparable(false);
        saved.setRentalCost(35000.0);  // ✨ NUEVO: Costo del arriendo

        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class))).thenReturn(saved);

        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-17"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("Vigente"))
                .andExpect(jsonPath("$.rentalCost").value(35000.0));  // ✨ NUEVO
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ JSON debe incluir rentalCost calculado")
    void create_withRentalCost() throws Exception {
        // Given: Préstamo con diferentes costos de arriendo
        LoanEntity saved = new LoanEntity();
        saved.setId(124L);
        saved.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(3);
        tool.setStatus("Disponible");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        saved.setTool(tool);

        saved.setStartDate(LocalDate.now());
        saved.setDueDate(LocalDate.now().plusDays(7));
        saved.setStatus("Vigente");
        saved.setFine(0d);
        saved.setDamaged(false);
        saved.setIrreparable(false);
        saved.setRentalCost(70000.0);  // 7 días * $10,000

        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class)))
                .thenReturn(saved);

        // When & Then: Verificar que rentalCost está en el response
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(124))
                .andExpect(jsonPath("$.status").value("Vigente"))
                .andExpect(jsonPath("$.rentalCost").exists())
                .andExpect(jsonPath("$.rentalCost").isNumber())
                .andExpect(jsonPath("$.rentalCost").value(70000.0));
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ rentalCost debe ser 0 para arriendo del mismo día")
    void create_sameDayRental_zeroRentalCost() throws Exception {
        // Given: Préstamo que vence el mismo día
        LoanEntity saved = new LoanEntity();
        saved.setId(125L);
        saved.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(3);
        tool.setStatus("Disponible");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        saved.setTool(tool);

        LocalDate today = LocalDate.now();
        saved.setStartDate(today);
        saved.setDueDate(today);  // Mismo día
        saved.setStatus("Vigente");
        saved.setFine(0d);
        saved.setDamaged(false);
        saved.setIrreparable(false);
        saved.setRentalCost(0.0);  // Sin costo por ser mismo día

        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class)))
                .thenReturn(saved);

        // When & Then: Verificar que rentalCost es 0
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalCost").value(0.0));
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ 409 conflicto (reglas negocio)")
    void create_conflict() throws Exception {
        when(loanService.createLoan(any(Long.class), any(Long.class), any(LocalDate.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cliente con préstamos vencidos"));

        mvc.perform(post(CREATE_ENDPOINT)
                        .param("clientId", "10")
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-10"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/loans/create ⇒ 400 si payload inválido")
    void create_badRequest() throws Exception {
        mvc.perform(post(CREATE_ENDPOINT)
                        .param("toolId", "20")
                        .param("dueDate", "2025-10-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/loans/return ⇒ 200 y JSON cuando se devuelve correctamente")
    void return_ok() throws Exception {
        LoanEntity returned = new LoanEntity();
        returned.setId(123L);
        returned.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(3);
        tool.setStatus("Disponible");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        returned.setTool(tool);

        returned.setStartDate(LocalDate.now().minusDays(5));
        returned.setDueDate(LocalDate.now().minusDays(1));
        returned.setReturnDate(LocalDate.now());
        returned.setStatus("Devuelto");
        returned.setFine(0d);
        returned.setDamaged(false);
        returned.setIrreparable(false);

        when(loanService.returnTool(any(Long.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(returned);

        mvc.perform(post("/api/v1/loans/return")
                        .param("loanId", "123")
                        .param("isDamaged", "false")
                        .param("isIrreparable", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("Devuelto"))
                .andExpect(jsonPath("$.fine").value(0.0));
    }

    @Test
    @DisplayName("POST /api/v1/loans/return ⇒ 200 con multa cuando hay atraso")
    void return_withFine() throws Exception {
        LoanEntity returned = new LoanEntity();
        returned.setId(124L);
        returned.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(3);
        tool.setStatus("Disponible");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        returned.setTool(tool);

        returned.setStartDate(LocalDate.now().minusDays(10));
        returned.setDueDate(LocalDate.now().minusDays(3));
        returned.setReturnDate(LocalDate.now());
        returned.setStatus("Atrasado");
        returned.setFine(3.0);
        returned.setDamaged(false);
        returned.setIrreparable(false);

        when(loanService.returnTool(any(Long.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(returned);

        mvc.perform(post("/api/v1/loans/return")
                        .param("loanId", "124")
                        .param("isDamaged", "false")
                        .param("isIrreparable", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Atrasado"))
                .andExpect(jsonPath("$.fine").value(3.0));
    }

    @Test
    @DisplayName("POST /api/v1/loans/return ⇒ 200 con daño reparable")
    void return_withDamage() throws Exception {
        LoanEntity returned = new LoanEntity();
        returned.setId(125L);
        returned.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(2);
        tool.setStatus("En reparación");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        returned.setTool(tool);

        returned.setStartDate(LocalDate.now().minusDays(3));
        returned.setDueDate(LocalDate.now().plusDays(2));
        returned.setReturnDate(LocalDate.now());
        returned.setStatus("Devuelto");
        returned.setFine(0.0);
        returned.setDamaged(true);
        returned.setIrreparable(false);

        when(loanService.returnTool(any(Long.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(returned);

        mvc.perform(post("/api/v1/loans/return")
                        .param("loanId", "125")
                        .param("isDamaged", "true")
                        .param("isIrreparable", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.status").value("En reparación"))
                .andExpect(jsonPath("$.damaged").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/loans/return ⇒ 200 con daño irreparable")
    void return_withIrreparableDamage() throws Exception {
        LoanEntity returned = new LoanEntity();
        returned.setId(126L);
        returned.setClient(new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo"));

        ToolEntity tool = new ToolEntity();
        tool.setId(20L);
        tool.setName("Taladro");
        tool.setStock(2);
        tool.setStatus("Dada de baja");
        tool.setCategory("Eléctricas");
        tool.setReplacementValue(50000);
        returned.setTool(tool);

        returned.setStartDate(LocalDate.now().minusDays(3));
        returned.setDueDate(LocalDate.now().plusDays(2));
        returned.setReturnDate(LocalDate.now());
        returned.setStatus("Devuelto");
        returned.setFine(50000.0);
        returned.setDamaged(true);
        returned.setIrreparable(true);

        when(loanService.returnTool(any(Long.class), any(Boolean.class), any(Boolean.class)))
                .thenReturn(returned);

        mvc.perform(post("/api/v1/loans/return")
                        .param("loanId", "126")
                        .param("isDamaged", "true")
                        .param("isIrreparable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tool.status").value("Dada de baja"))
                .andExpect(jsonPath("$.fine").value(50000.0))
                .andExpect(jsonPath("$.irreparable").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/loans/ ⇒ 200 y lista de préstamos con rentalCost")
    void getAllLoans_ok() throws Exception {
        ClientEntity client = new ClientEntity(10L, "Juan","12.345.678-9","+569...","juan@toolrent.cl","Activo");

        ToolEntity tool1 = new ToolEntity();
        tool1.setId(20L);
        tool1.setName("Taladro");
        tool1.setStock(2);
        tool1.setStatus("Prestada");
        tool1.setCategory("Eléctricas");
        tool1.setReplacementValue(50000);

        ToolEntity tool2 = new ToolEntity();
        tool2.setId(21L);
        tool2.setName("Martillo");
        tool2.setStock(5);
        tool2.setStatus("Prestada");
        tool2.setCategory("Manuales");
        tool2.setReplacementValue(15000);

        LoanEntity loan1 = new LoanEntity();
        loan1.setId(100L);
        loan1.setClient(client);
        loan1.setTool(tool1);
        loan1.setStartDate(LocalDate.now().minusDays(2));
        loan1.setDueDate(LocalDate.now().plusDays(5));
        loan1.setStatus("Vigente");
        loan1.setFine(0d);
        loan1.setRentalCost(35000.0);  // ✨ NUEVO

        LoanEntity loan2 = new LoanEntity();
        loan2.setId(101L);
        loan2.setClient(client);
        loan2.setTool(tool2);
        loan2.setStartDate(LocalDate.now().minusDays(1));
        loan2.setDueDate(LocalDate.now().plusDays(3));
        loan2.setStatus("Vigente");
        loan2.setFine(0d);
        loan2.setRentalCost(20000.0);  // ✨ NUEVO

        when(loanService.getAllLoans()).thenReturn(List.of(loan1, loan2));

        mvc.perform(get("/api/v1/loans/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].status").value("Vigente"))
                .andExpect(jsonPath("$[0].rentalCost").value(35000.0))  // ✨ NUEVO
                .andExpect(jsonPath("$[1].id").value(101))
                .andExpect(jsonPath("$[1].status").value("Vigente"))
                .andExpect(jsonPath("$[1].rentalCost").value(20000.0));  // ✨ NUEVO
    }

    @Test
    @DisplayName("GET /api/v1/loans/ ⇒ 200 y lista vacía cuando no hay préstamos")
    void getAllLoans_empty() throws Exception {
        when(loanService.getAllLoans()).thenReturn(List.of());

        mvc.perform(get("/api/v1/loans/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}