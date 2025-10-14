package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador para Reportes y Consultas
 * Épica 6: Reportes y consultas
 *
 * Brinda capacidad de generar reportes sobre:
 * - Préstamos activos y su estado
 * - Clientes con atrasos
 * - Herramientas más prestadas (ranking)
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reportes y consultas del sistema (Épica 6)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ToolRepository toolRepository;

    /**
     * RF6.1: Listar préstamos activos y su estado (vigentes, atrasados)
     * GET /api/v1/reports/active-loans
     *
     * Retorna todos los préstamos que NO han sido devueltos (returnDate = null)
     * Los préstamos pueden estar en estado "Vigente" o "Atrasado"
     *
     * Filtros opcionales:
     * - startDate: Fecha de inicio del rango
     * - endDate: Fecha de fin del rango
     */
    @GetMapping("/active-loans")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Obtener préstamos activos (vigentes y atrasados)",
            description = "RF6.1: Lista todos los préstamos que no han sido devueltos, " +
                    "con filtros opcionales por rango de fechas")
    public ResponseEntity<List<LoanEntity>> getActiveLoans(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<LoanEntity> activeLoans;

        if (startDate != null && endDate != null) {
            // Filtrar por rango de fechas
            activeLoans = loanRepository.findActiveLoansByDateRange(startDate, endDate);
        } else {
            // Sin filtro de fechas, traer todos los activos
            activeLoans = loanRepository.findActiveLoans();
        }

        return ResponseEntity.ok(activeLoans);
    }

    /**
     * RF6.2: Listar clientes con atrasos
     * GET /api/v1/reports/clients-with-overdues
     *
     * Retorna lista de clientes que tienen préstamos vencidos sin devolver
     * (dueDate < hoy AND returnDate = null)
     */
    @GetMapping("/clients-with-overdues")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Obtener clientes con préstamos atrasados",
            description = "RF6.2: Lista todos los clientes que tienen préstamos " +
                    "vencidos sin devolver")
    public ResponseEntity<List<ClientEntity>> getClientsWithOverdues() {
        List<ClientEntity> clients = clientRepository.findClientsWithOverdues();
        return ResponseEntity.ok(clients);
    }

    /**
     * RF6.3: Reporte de las herramientas más prestadas (Ranking)
     * GET /api/v1/reports/most-loaned-tools
     *
     * Retorna ranking de herramientas ordenadas por cantidad de préstamos
     *
     * Filtros opcionales:
     * - startDate: Fecha de inicio para contar préstamos
     * - endDate: Fecha de fin para contar préstamos
     * - limit: Cantidad de resultados (default: 10)
     */
    @GetMapping("/most-loaned-tools")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ToolRanking>> getMostLoanedTools(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ToolRanking> ranking;  // ← Aquí dentro de la clase puedes usar solo "ToolRanking"

        if (startDate != null && endDate != null) {
            ranking = toolRepository.findMostLoanedToolsByDateRange(
                    startDate, endDate, PageRequest.of(0, limit)
            );
        } else {
            ranking = toolRepository.findMostLoanedTools(PageRequest.of(0, limit));
        }

        return ResponseEntity.ok(ranking);
    }
    @Setter
    @Getter
    public static class ToolRanking {
        private Long toolId;
        private String toolName;
        private Long loanCount;

        public ToolRanking(Long toolId, String toolName, Long loanCount) {
            this.toolId = toolId;
            this.toolName = toolName;
            this.loanCount = loanCount;
        }

    }
}