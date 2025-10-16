package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Service.ConfigService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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

    // ✅ NUEVO: Inyectar ConfigService para obtener multa por día
    @Autowired
    private ConfigService configService;

    /**
     * RF6.1: Listar préstamos activos y su estado (vigentes, atrasados)
     * GET /api/v1/reports/active-loans
     *
     * Retorna todos los préstamos que NO han sido devueltos (returnDate = null)
     * Los préstamos pueden estar en estado "Vigente" o "Atrasado"
     * ✅ MODIFICADO: Calcula multas dinámicamente usando la configuración del sistema
     *
     * Filtros opcionales:
     * - startDate: Fecha de inicio del rango
     * - endDate: Fecha de fin del rango
     */
    @GetMapping("/active-loans")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Obtener préstamos activos (vigentes y atrasados)",
            description = "RF6.1: Lista todos los préstamos que no han sido devueltos, " +
                    "con filtros opcionales por rango de fechas. Calcula multas automáticamente.")
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

        // ✅ NUEVO: Calcular multas dinámicamente y actualizar estado
        Double multaPorDia = configService.getTarifaMultaDiaria();
        LocalDate hoy = LocalDate.now();

        activeLoans = activeLoans.stream().map(loan -> {
            // Calcular días de atraso
            long diasAtraso = 0;
            Double multaCalculada = 0.0;
            String estadoActualizado = "Vigente";

            if (hoy.isAfter(loan.getDueDate())) {
                // Préstamo atrasado
                diasAtraso = ChronoUnit.DAYS.between(loan.getDueDate(), hoy);
                multaCalculada = diasAtraso * multaPorDia;
                estadoActualizado = "Atrasado";
            }

            // Actualizar valores calculados (no se persisten en BD, solo para la respuesta)
            loan.setFine(multaCalculada);
            loan.setStatus(estadoActualizado);

            return loan;
        }).collect(Collectors.toList());

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
     * Parámetros opcionales:
     * - startDate: Fecha inicio del rango
     * - endDate: Fecha fin del rango
     * - limit: Cantidad de resultados (default: 10)
     */
    @GetMapping("/most-loaned-tools")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Obtener ranking de herramientas más prestadas",
            description = "RF6.3: Lista las herramientas ordenadas por cantidad de préstamos, " +
                    "con filtros opcionales por rango de fechas")
    public ResponseEntity<List<ToolRanking>> getMostLoanedTools(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ToolRanking> ranking;

        if (startDate != null && endDate != null) {
            // Ranking filtrado por fechas
            ranking = toolRepository.findMostLoanedToolsByDateRange(
                    startDate, endDate, PageRequest.of(0, limit)
            );
        } else {
            // Ranking global (todos los préstamos históricos)
            ranking = toolRepository.findMostLoanedTools(PageRequest.of(0, limit));
        }

        return ResponseEntity.ok(ranking);
    }

    /**
     * DTO para el ranking de herramientas
     */
    @Getter
    @Setter
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