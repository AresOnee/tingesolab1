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
    @Autowired private KardexService kardexService; // ✅ NUEVA DEPENDENCIA

    /**
     * RF2.1: Crea un préstamo aplicando reglas de negocio
     * RF5.1: Registrar automáticamente en kardex
     */
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate) {
        if (clientId == null || toolId == null || dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId, toolId y dueDate son obligatorios");
        }

        // 1) Cargar entidades relacionadas
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herramienta no encontrada"));

        // 2) Reglas de negocio
        LocalDate today = LocalDate.now();
        List<LoanEntity> allLoans = loanRepository.findAll();

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

        // 2.1) Cliente no debe tener préstamos vencidos
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

        // Calcular costo del arriendo
        double costoArriendo = configService.getTarifaArriendoDiaria();

        // 3) Crear préstamo
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(today);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("Vigente");
        loan.setFine(0d);
        loan.setRentalCost(costoArriendo);
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

                // ✅ RF5.1: Registrar baja por daño irreparable
                kardexService.registerMovement(
                        tool.getId(),
                        "BAJA",
                        -1, // se pierde la unidad
                        "USER",
                        "Baja por daño irreparable en préstamo #" + loan.getId(),
                        loan.getId()
                );
            } else {
                // Queda en reparación: no vuelve al stock por ahora
                tool.setStatus("En reparación");

                // ✅ RF5.1: Registrar movimiento a reparación
                kardexService.registerMovement(
                        tool.getId(),
                        "REPARACION",
                        0, // no cambia stock, solo estado
                        "USER",
                        "Herramienta en reparación por daños en préstamo #" + loan.getId(),
                        loan.getId()
                );
            }
        } else {
            // Devuelta en buen estado
            tool.setStock(tool.getStock() + 1);
            tool.setStatus("Disponible");

            // ✅ RF5.1: Registrar devolución exitosa
            kardexService.registerMovement(
                    tool.getId(),
                    "DEVOLUCION",
                    1, // positivo porque incrementa stock
                    "USER",
                    "Devolución en buen estado de préstamo #" + loan.getId(),
                    loan.getId()
            );
        }

        toolRepository.save(tool);
        return loanRepository.save(loan);
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }
}