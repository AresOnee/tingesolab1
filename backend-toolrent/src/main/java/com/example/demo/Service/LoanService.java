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

    /**
     * Crea un préstamo aplicando reglas de negocio.
     * Ajusta la firma si tu controlador te envía un DTO distinto;
     * internamente debes terminar llamando a este método con clientId, toolId y dueDate.
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La herramienta no está disponible para préstamo"
            );
        }

// 2.0.2) Sin stock (mover esto antes de las otras reglas)
        if (tool.getStock() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Herramienta sin stock");
        }

// 2.1) Cliente con préstamos vencidos (returnDate == null y dueDate < hoy)
        boolean hasOverdue = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId))
                .anyMatch(l -> l.getReturnDate() == null
                        && l.getDueDate() != null
                        && l.getDueDate().isBefore(today));
        if (hasOverdue) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cliente con préstamos vencidos");
        }

// 2.2) Máximo 5 préstamos activos por cliente (returnDate == null)
        long activeCount = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId))
                .filter(l -> l.getReturnDate() == null)
                .count();
        if (activeCount >= 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Máximo 5 préstamos activos por cliente");
        }

// 2.3) Evitar que el cliente tenga la misma herramienta activa en paralelo
        boolean duplicateTool = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId))
                .filter(l -> Objects.equals(l.getTool().getId(), toolId))
                .anyMatch(l -> l.getReturnDate() == null);
        if (duplicateTool) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un préstamo activo de esta misma herramienta para el cliente"
            );
        }

        // 2.4) Sin stock
        // 2.4) Sin stock
        int stockActual = tool.getStock();      // ← int no puede ser null
        if (stockActual <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Herramienta sin stock");
        }



        // 3) Crear préstamo (usando asociaciones) y descontar stock
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(today);
        loan.setDueDate(dueDate);
        loan.setReturnDate(null);
        loan.setStatus("Vigente"); // o "ACTIVO" si así lo manejas
        loan.setFine(0d);
        loan.setDamaged(false);
        loan.setIrreparable(false);

        // Descontar stock y persistir
        tool.setStock(stockActual - 1);

        LoanEntity saved = loanRepository.save(loan);
        toolRepository.save(tool); // guarda nuevo stock

        return saved;
    }


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
        // La tarifa diaria de multa se parametriza más adelante; por ahora usar 1.0 como placeholder
        double fineDaily = 1.0;
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
            } else {
                // Queda en reparación: no vuelve al stock por ahora
                tool.setStatus("En reparación");
            }
        } else {
            // Devuelta en buen estado
            tool.setStock(tool.getStock() + 1);
            tool.setStatus("Disponible");
        }

        toolRepository.save(tool);
        return loanRepository.save(loan);
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }
}
