package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class LoanService {

    @Autowired private LoanRepository loanRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private ToolRepository toolRepository;
    @Autowired private ConfigService configService;
    @Autowired private KardexService kardexService;

    /**
     * RF2.1: Crea un préstamo aplicando reglas de negocio
     * RF5.1: Registrar automáticamente en kardex
     *
     * ✅ MEJORAS IMPLEMENTADAS (Gap Analysis - Punto 4):
     * - Validación de estado del cliente (Activo/Restringido)
     * - Validación de multas pendientes usando hasOverduesOrFines()
     * - Validación de fechas (dueDate > startDate)
     * - Cálculo correcto de rentalCost (días × tarifa)
     */
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate) {
        if (clientId == null || toolId == null || dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "clientId, toolId y dueDate son obligatorios");
        }

        // 1) Cargar entidades relacionadas
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente no encontrado"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Herramienta no encontrada"));

        // 2) Reglas de negocio
        LocalDate today = LocalDate.now();
        List<LoanEntity> allLoans = loanRepository.findAll();

        // ✅ NUEVA VALIDACIÓN 1: Cliente debe estar en estado "Activo"
        // RN: "El cliente debe estar en estado Activo (no restringido)"
        if (!"Activo".equalsIgnoreCase(client.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente está en estado '%s'. Solo clientes activos pueden solicitar préstamos.",
                            client.getState()));
        }

        // ✅ NUEVA VALIDACIÓN 2: Cliente no debe tener multas pendientes
        // RN: "El cliente no debe tener multas impagas"
        if (loanRepository.hasOverduesOrFines(clientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cliente tiene multas pendientes o préstamos atrasados sin regularizar. " +
                            "Debe pagar las multas antes de solicitar un nuevo préstamo.");
        }

        // ✅ NUEVA VALIDACIÓN 3: Fecha de devolución debe ser posterior a la fecha de inicio
        // RN: "El sistema debe verificar que la fecha de devolución no sea anterior a la fecha de entrega"
        if (dueDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de devolución no puede ser anterior a la fecha de préstamo");
        }

        // 2.0.1) Herramienta debe estar "Disponible"
        if (!"Disponible".equalsIgnoreCase(tool.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La herramienta no está disponible (estado: " + tool.getStatus() + ")");
        }

        // 2.0.2) Stock >= 1
        int stockActual = tool.getStock();
        if (stockActual < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La herramienta está sin stock");
        }

        // 2.1) Cliente no debe tener préstamos vencidos (esta validación ya está cubierta por hasOverduesOrFines)
        // Pero la mantenemos por compatibilidad con tests existentes
        boolean tieneVencidos = allLoans.stream()
                .anyMatch(l ->
                        Objects.equals(l.getClient().getId(), clientId)
                                && l.getReturnDate() == null
                                && l.getDueDate().isBefore(today)
                );
        if (tieneVencidos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cliente tiene préstamos vencidos sin devolver");
        }

        // 2.2) Máximo 5 préstamos activos
        long activos = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId) && l.getReturnDate() == null)
                .count();
        if (activos >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Máximo 5 préstamos activos por cliente");
        }

        // 2.3) No puede tener la misma herramienta activa
        boolean mismaTool = allLoans.stream()
                .anyMatch(l ->
                        Objects.equals(l.getClient().getId(), clientId)
                                && Objects.equals(l.getTool().getId(), toolId)
                                && l.getReturnDate() == null
                );
        if (mismaTool) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cliente ya tiene un préstamo activo de la misma herramienta");
        }

        // ✅ CORRECCIÓN: Calcular costo del arriendo correctamente
        // RN: "costo del arriendo = días × tarifa"
        double tarifaDiaria = configService.getTarifaArriendoDiaria();
        long dias = ChronoUnit.DAYS.between(today, dueDate);

        // Mínimo 1 día de arriendo (según RN)
        if (dias < 1) {
            dias = 1;
        }

        double costoArriendo = dias * tarifaDiaria;

        // 3) Crear préstamo
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(today);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("Vigente");
        loan.setFine(0d);
        loan.setRentalCost(costoArriendo); // ✅ CORRECCIÓN: Ahora multiplica por días
        loan.setDamaged(false);
        loan.setIrreparable(false);

        // Descontar stock y persistir
        tool.setStock(stockActual - 1);

        LoanEntity saved = loanRepository.save(loan);
        toolRepository.save(tool);

        // ✅ RF5.1: Registrar préstamo en kardex
        kardexService.registerMovement(
                tool.getId(),
                "PRESTAMO",
                -1, // negativo porque reduce stock
                "USER", // o extraer del JWT si está disponible
                "Préstamo a cliente: " + client.getName(),
                saved.getId()
        );

        return saved;
    }

    /**
     * RF2.3: Registrar devolución de herramienta
     * RF5.1: Registrar automáticamente en kardex
     */
    @Transactional
    public LoanEntity returnTool(Long loanId, boolean isDamaged, boolean isIrreparable) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        var tool = loan.getTool();

        if ("Devuelto".equalsIgnoreCase(loan.getStatus())) {
            return loan;
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);

        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), today));
        double fineDaily = configService.getTarifaMultaDiaria();

        if (daysLate > 0) {
            loan.setFine((loan.getFine() == null ? 0.0 : loan.getFine()) + daysLate * fineDaily);
            loan.setStatus("Atrasado");
        } else {
            loan.setStatus("Devuelto");
        }

        if (isDamaged) {
            if (isIrreparable) {
                // Baja definitiva: no retorna al stock, se cobra reposición
                tool.setStatus("Dada de baja");
                loan.setFine((loan.getFine() == null ? 0.0 : loan.getFine()) + tool.getReplacementValue());
                loan.setIrreparable(true);

                kardexService.registerMovement(
                        tool.getId(),
                        "BAJA",
                        0,
                        "USER",
                        "Baja por daño irreparable (loan #" + loanId + ")",
                        loanId
                );
            } else {
                tool.setStatus("En reparación");
                tool.setStock(tool.getStock() + 1);

                kardexService.registerMovement(
                        tool.getId(),
                        "DEVOLUCION",
                        1,
                        "USER",
                        "Devolución con daño reparable (loan #" + loanId + ")",
                        loanId
                );
            }
            loan.setDamaged(true);
        } else {
            tool.setStock(tool.getStock() + 1);

            kardexService.registerMovement(
                    tool.getId(),
                    "DEVOLUCION",
                    1,
                    "USER",
                    "Devolución normal (loan #" + loanId + ")",
                    loanId
            );
        }

        toolRepository.save(tool);
        return loanRepository.save(loan);
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }
}