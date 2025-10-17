package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import com.example.demo.Service.ConfigService;
import com.example.demo.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests para ReportController - Épica 6
 * Cobertura de los 3 endpoints de reportes
 */
@WebMvcTest(controllers = ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LoanRepository loanRepository;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private ToolRepository toolRepository;

    // ✅ AGREGAR: MockBean para ConfigService
    @MockBean
    private ConfigService configService;

    // ========== HELPERS ==========

    private LoanEntity createLoan(Long id, String status, LocalDate startDate, LocalDate dueDate) {
        LoanEntity loan = new LoanEntity();
        loan.setId(id);
        loan.setStatus(status);
        loan.setStartDate(startDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setFine(0.0);

        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setName("Juan Perez");
        loan.setClient(client);

        ToolEntity tool = new ToolEntity();
        tool.setId(1L);
        tool.setName("Taladro");
        loan.setTool(tool);

        return loan;
    }

    private ClientEntity createClient(Long id, String name, String rut) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(name);
        client.setRut(rut);
        client.setEmail("test@test.cl");
        client.setPhone("+56911111111");
        client.setState("Activo");
        return client;
    }

    private ReportController.ToolRanking createToolRanking(Long toolId, String name, Long count) {
        return new ReportController.ToolRanking(toolId, name, count);
    }

    // ========== TESTS RF6.1: PRÉSTAMOS ACTIVOS ==========

    @Test
    @DisplayName("RF6.1: GET /api/v1/reports/active-loans sin filtros => 200 con lista de préstamos activos")
    @WithMockUser(roles = "USER")
    void getActiveLoans_noFilters_returnsActiveLoans() throws Exception {
        // Given: 2 préstamos activos
        List<LoanEntity> activeLoans = List.of(
                createLoan(1L, "Vigente", LocalDate.now().minusDays(2), LocalDate.now().plusDays(5)),
                createLoan(2L, "Atrasado", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
        );

        when(loanRepository.findActiveLoans()).thenReturn(activeLoans);
        // ✅ AGREGAR: Mock para getTarifaMultaDiaria
        when(configService.getTarifaMultaDiaria()).thenReturn(6000.0);

        // When & Then
        mvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(loanRepository).findActiveLoans();
        verify(configService).getTarifaMultaDiaria();
    }

    @Test
    @DisplayName("RF6.1: GET /api/v1/reports/active-loans con filtros de fecha => usa query con rango")
    @WithMockUser(roles = "USER")
    void getActiveLoans_withDateFilters_usesDateRangeQuery() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        List<LoanEntity> loans = List.of(
                createLoan(1L, "Vigente", startDate, startDate.plusDays(7))
        );

        when(loanRepository.findActiveLoansByDateRange(startDate, endDate)).thenReturn(loans);
        when(configService.getTarifaMultaDiaria()).thenReturn(6000.0);

        // When & Then
        mvc.perform(get("/api/v1/reports/active-loans")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(loanRepository).findActiveLoansByDateRange(startDate, endDate);
        verify(loanRepository, never()).findActiveLoans();
        verify(configService).getTarifaMultaDiaria();
    }

    @Test
    @DisplayName("RF6.1: GET /api/v1/reports/active-loans sin préstamos activos => lista vacía")
    @WithMockUser(roles = "USER")
    void getActiveLoans_noActiveLoans_returnsEmptyList() throws Exception {
        // Given: Sin préstamos activos
        when(loanRepository.findActiveLoans()).thenReturn(new ArrayList<>());
        when(configService.getTarifaMultaDiaria()).thenReturn(6000.0);

        // When & Then
        mvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(loanRepository).findActiveLoans();
    }

    @Test
    @DisplayName("RF6.1: GET /api/v1/reports/active-loans sin autenticación => 401")
    void getActiveLoans_unauthorized_returns401() throws Exception {
        // When & Then
        mvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isUnauthorized());

        verify(loanRepository, never()).findActiveLoans();
    }

    @Test
    @DisplayName("RF6.1: GET /api/v1/reports/active-loans con rol ADMIN => 200")
    @WithMockUser(roles = "ADMIN")
    void getActiveLoans_asAdmin_returns200() throws Exception {
        // Given
        when(loanRepository.findActiveLoans()).thenReturn(new ArrayList<>());
        when(configService.getTarifaMultaDiaria()).thenReturn(6000.0);

        // When & Then
        mvc.perform(get("/api/v1/reports/active-loans"))
                .andExpect(status().isOk());

        verify(loanRepository).findActiveLoans();
    }

    // ========== TESTS RF6.2: CLIENTES CON ATRASOS ==========

    @Test
    @DisplayName("RF6.2: GET /api/v1/reports/clients-with-overdues => 200 con lista de clientes")
    @WithMockUser(roles = "USER")
    void getClientsWithOverdues_returnsClients() throws Exception {
        // Given: 2 clientes con atrasos
        List<ClientEntity> clients = List.of(
                createClient(1L, "Juan Perez", "12.345.678-9"),
                createClient(2L, "Maria Lopez", "98.765.432-1")
        );

        when(clientRepository.findClientsWithOverdues()).thenReturn(clients);

        // When & Then
        mvc.perform(get("/api/v1/reports/clients-with-overdues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Juan Perez"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Maria Lopez"));

        verify(clientRepository).findClientsWithOverdues();
    }

    @Test
    @DisplayName("RF6.2: GET /api/v1/reports/clients-with-overdues sin clientes => lista vacía")
    @WithMockUser(roles = "USER")
    void getClientsWithOverdues_noClients_returnsEmptyList() throws Exception {
        // Given: Sin clientes con atrasos
        when(clientRepository.findClientsWithOverdues()).thenReturn(new ArrayList<>());

        // When & Then
        mvc.perform(get("/api/v1/reports/clients-with-overdues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(clientRepository).findClientsWithOverdues();
    }

    @Test
    @DisplayName("RF6.2: GET /api/v1/reports/clients-with-overdues sin autenticación => 401")
    void getClientsWithOverdues_unauthorized_returns401() throws Exception {
        // When & Then
        mvc.perform(get("/api/v1/reports/clients-with-overdues"))
                .andExpect(status().isUnauthorized());

        verify(clientRepository, never()).findClientsWithOverdues();
    }

    @Test
    @DisplayName("RF6.2: GET /api/v1/reports/clients-with-overdues con rol ADMIN => 200")
    @WithMockUser(roles = "ADMIN")
    void getClientsWithOverdues_asAdmin_returns200() throws Exception {
        // Given
        when(clientRepository.findClientsWithOverdues()).thenReturn(new ArrayList<>());

        // When & Then
        mvc.perform(get("/api/v1/reports/clients-with-overdues"))
                .andExpect(status().isOk());

        verify(clientRepository).findClientsWithOverdues();
    }

    // ========== TESTS RF6.3: RANKING HERRAMIENTAS ==========

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools sin filtros => top 10 por defecto")
    @WithMockUser(roles = "USER")
    void getMostLoanedTools_noFilters_returnsTop10() throws Exception {
        // Given: Top 3 herramientas
        List<ReportController.ToolRanking> ranking = List.of(
                createToolRanking(1L, "Taladro", 25L),
                createToolRanking(2L, "Martillo", 18L),
                createToolRanking(3L, "Sierra", 12L)
        );

        when(toolRepository.findMostLoanedTools(any(PageRequest.class)))
                .thenReturn(ranking);

        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].toolId").value(1))
                .andExpect(jsonPath("$[0].toolName").value("Taladro"))
                .andExpect(jsonPath("$[0].loanCount").value(25))
                .andExpect(jsonPath("$[1].toolId").value(2))
                .andExpect(jsonPath("$[1].loanCount").value(18))
                .andExpect(jsonPath("$[2].toolId").value(3))
                .andExpect(jsonPath("$[2].loanCount").value(12));

        verify(toolRepository).findMostLoanedTools(PageRequest.of(0, 10));
        verify(toolRepository, never()).findMostLoanedToolsByDateRange(any(), any(), any());
    }

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools con limit personalizado => usa limit")
    @WithMockUser(roles = "USER")
    void getMostLoanedTools_withCustomLimit_usesLimit() throws Exception {
        // Given: Top 5 herramientas
        List<ReportController.ToolRanking> ranking = List.of(
                createToolRanking(1L, "Taladro", 25L)
        );

        when(toolRepository.findMostLoanedTools(PageRequest.of(0, 5)))
                .thenReturn(ranking);

        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(toolRepository).findMostLoanedTools(PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools con filtros de fecha => usa query con rango")
    @WithMockUser(roles = "USER")
    void getMostLoanedTools_withDateFilters_usesDateRangeQuery() throws Exception {
        // Given: Ranking filtrado por fecha
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        List<ReportController.ToolRanking> ranking = List.of(
                createToolRanking(1L, "Taladro", 10L)
        );

        when(toolRepository.findMostLoanedToolsByDateRange(
                eq(startDate), eq(endDate), any(PageRequest.class)))
                .thenReturn(ranking);

        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].toolId").value(1))
                .andExpect(jsonPath("$[0].loanCount").value(10));

        verify(toolRepository).findMostLoanedToolsByDateRange(
                startDate, endDate, PageRequest.of(0, 10)
        );
        verify(toolRepository, never()).findMostLoanedTools(any());
    }

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools sin herramientas => lista vacía")
    @WithMockUser(roles = "USER")
    void getMostLoanedTools_noTools_returnsEmptyList() throws Exception {
        // Given: Sin herramientas prestadas
        when(toolRepository.findMostLoanedTools(any(PageRequest.class)))
                .thenReturn(new ArrayList<>());

        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(toolRepository).findMostLoanedTools(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools sin autenticación => 401")
    void getMostLoanedTools_unauthorized_returns401() throws Exception {
        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools"))
                .andExpect(status().isUnauthorized());

        verify(toolRepository, never()).findMostLoanedTools(any());
    }

    @Test
    @DisplayName("RF6.3: GET /api/v1/reports/most-loaned-tools con rol ADMIN => 200")
    @WithMockUser(roles = "ADMIN")
    void getMostLoanedTools_asAdmin_returns200() throws Exception {
        // Given
        when(toolRepository.findMostLoanedTools(any(PageRequest.class)))
                .thenReturn(new ArrayList<>());

        // When & Then
        mvc.perform(get("/api/v1/reports/most-loaned-tools"))
                .andExpect(status().isOk());

        verify(toolRepository).findMostLoanedTools(PageRequest.of(0, 10));
    }

    // ========== TESTS DE CLASE INTERNA TOOLRANKING ==========

    @Test
    @DisplayName("ToolRanking: Constructor y getters funcionan correctamente")
    void toolRanking_constructorAndGetters() {
        // When
        ReportController.ToolRanking ranking = new ReportController.ToolRanking(
                1L, "Taladro", 25L
        );

        // Then
        assert ranking.getToolId().equals(1L);
        assert ranking.getToolName().equals("Taladro");
        assert ranking.getLoanCount().equals(25L);
    }

    @Test
    @DisplayName("ToolRanking: Setters funcionan correctamente")
    void toolRanking_setters() {
        // Given
        ReportController.ToolRanking ranking = new ReportController.ToolRanking(
                1L, "Taladro", 25L
        );

        // When
        ranking.setToolId(2L);
        ranking.setToolName("Martillo");
        ranking.setLoanCount(30L);

        // Then
        assert ranking.getToolId().equals(2L);
        assert ranking.getToolName().equals("Martillo");
        assert ranking.getLoanCount().equals(30L);
    }
}