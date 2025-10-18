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
     * ✅ MEJORADO: Mensajes de error específicos y detallados
     */
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, String username) {
        // ========================================
        // VALIDACIONES BÁSICAS
        // ========================================
        if (clientId == null || toolId == null || dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los campos cliente, herramienta y fecha de devolución son obligatorios");
        }

        // ========================================
        // CARGAR ENTIDADES
        // ========================================
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Cliente con ID %d no encontrado", clientId)));

        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Herramienta con ID %d no encontrada", toolId)));

        // ========================================
        // VALIDACIONES DE NEGOCIO CON MENSAJES ESPECÍFICOS
        // ========================================
        LocalDate today = LocalDate.now();
        List<LoanEntity> allLoans = loanRepository.findAll();

        // ✅ VALIDACIÓN 1: Cliente debe estar en estado "Activo"
        if (!"Activo".equalsIgnoreCase(client.getState())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente '%s' está en estado '%s'. Solo se pueden realizar préstamos a clientes en estado 'Activo'.",
                            client.getName(), client.getState()));
        }

        // ✅ VALIDACIÓN 2: Cliente no debe tener préstamos atrasados o multas pendientes
        if (loanRepository.hasOverduesOrFines(clientId)) {
            // Buscar préstamos con problemas para dar detalles
            List<LoanEntity> problematicLoans = allLoans.stream()
                    .filter(l -> Objects.equals(l.getClient().getId(), clientId))
                    .filter(l ->
                            // Préstamos atrasados sin devolver
                            (l.getReturnDate() == null && l.getDueDate().isBefore(today)) ||
                                    // Préstamos con multas pendientes
                                    (l.getFine() != null && l.getFine() > 0 && "Atrasado".equals(l.getStatus()))
                    )
                    .toList();

            if (!problematicLoans.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder();
                errorMessage.append(String.format("El cliente '%s' tiene los siguientes problemas pendientes:\n", client.getName()));

                for (LoanEntity loan : problematicLoans) {
                    if (loan.getReturnDate() == null && loan.getDueDate().isBefore(today)) {
                        long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                        errorMessage.append(String.format(
                                "• Préstamo #%d (%s) vencido hace %d día(s)\n",
                                loan.getId(),
                                loan.getTool().getName(),
                                daysOverdue
                        ));
                    }

                    if (loan.getFine() != null && loan.getFine() > 0 && "Atrasado".equals(loan.getStatus())) {
                        errorMessage.append(String.format(
                                "• Préstamo #%d (%s) con multa pendiente de $%.0f\n",
                                loan.getId(),
                                loan.getTool().getName(),
                                loan.getFine()
                        ));
                    }
                }

                errorMessage.append("\nDebe regularizar su situación antes de solicitar un nuevo préstamo.");

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage.toString());
            }
        }

        // ✅ VALIDACIÓN 3: Fecha de devolución debe ser posterior a hoy
        if (dueDate.isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("La fecha de devolución (%s) no puede ser anterior a la fecha actual (%s)",
                            dueDate, today));
        }

        // ✅ VALIDACIÓN 4: Herramienta debe estar "Disponible"
        if (!"Disponible".equalsIgnoreCase(tool.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("La herramienta '%s' no está disponible. Estado actual: '%s'",
                            tool.getName(), tool.getStatus()));
        }

        // ✅ VALIDACIÓN 5: Stock >= 1
        int stockActual = tool.getStock();
        if (stockActual < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("La herramienta '%s' no tiene stock disponible (Stock actual: %d)",
                            tool.getName(), stockActual));
        }

        // ✅ VALIDACIÓN 6: Cliente no debe tener préstamos vencidos sin devolver
        boolean tieneVencidos = allLoans.stream()
                .anyMatch(l ->
                        Objects.equals(l.getClient().getId(), clientId)
                                && l.getReturnDate() == null
                                && l.getDueDate().isBefore(today)
                );
        if (tieneVencidos) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente '%s' tiene préstamos vencidos sin devolver. " +
                                    "Debe devolver las herramientas atrasadas antes de solicitar un nuevo préstamo.",
                            client.getName()));
        }

        // ✅ VALIDACIÓN 7: Máximo 5 préstamos activos
        long activos = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId) && l.getReturnDate() == null)
                .count();
        if (activos >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente '%s' ya tiene 5 préstamos activos. " +
                                    "Debe devolver al menos una herramienta antes de solicitar un nuevo préstamo.",
                            client.getName()));
        }

        // ✅ VALIDACIÓN 8: No puede tener la misma herramienta activa
        boolean mismaTool = allLoans.stream()
                .anyMatch(l ->
                        Objects.equals(l.getClient().getId(), clientId)
                                && Objects.equals(l.getTool().getId(), toolId)
                                && l.getReturnDate() == null
                );
        if (mismaTool) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente '%s' ya tiene un préstamo activo de la herramienta '%s'. " +
                                    "No se puede prestar la misma herramienta dos veces simultáneamente.",
                            client.getName(), tool.getName()));
        }

        // ========================================
        // CÁLCULO DE COSTO DE ARRIENDO
        // ========================================
        double tarifaDiaria = configService.getTarifaArriendoDiaria();
        long dias = ChronoUnit.DAYS.between(today, dueDate);

        // Mínimo 1 día de arriendo
        if (dias < 1) {
            dias = 1;
        }

        double costoArriendo = dias * tarifaDiaria;

        // ========================================
        // CREAR PRÉSTAMO
        // ========================================
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

        // ✅ RF5.1: Registrar préstamo en kardex con username real
        kardexService.registerMovement(
                tool.getId(),
                "PRESTAMO",
                -1,
                username,
                String.format("Préstamo a cliente: %s", client.getName()),
                saved.getId()
        );

        return saved;
    }

    /**
     * RF2.3: Registrar devolución de herramienta
     * RF2.4: Calcular multas por atraso
     * RF5.1: Registrar automáticamente en kardex
     */
    @Transactional
    public LoanEntity returnTool(Long loanId, boolean isDamaged, boolean isIrreparable, String username) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Préstamo con ID %d no encontrado", loanId)));
        var tool = loan.getTool();

        // Si ya está devuelto, no hacer nada
        if ("Devuelto".equalsIgnoreCase(loan.getStatus())) {
            return loan;
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);

        // 1) Calcular multa por atraso
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), today));
        double fineDaily = configService.getTarifaMultaDiaria();

        if (daysLate > 0) {
            loan.setFine((loan.getFine() == null ? 0 : loan.getFine()) + (daysLate * fineDaily));
        }

        int stockActual = tool.getStock();
        loan.setDamaged(isDamaged);
        loan.setIrreparable(isIrreparable);

        // 2) Procesar según tipo de daño
        if (isDamaged && isIrreparable) {
            // CASO 1: Daño IRREPARABLE
            tool.setStatus("Dada de baja");
            double repoValue = tool.getReplacementValue();
            loan.setFine((loan.getFine() == null ? 0 : loan.getFine()) + repoValue);

            kardexService.registerMovement(
                    tool.getId(),
                    "BAJA",
                    0,
                    username,
                    String.format("Baja por daño irreparable (préstamo #%d)", loanId),
                    loanId
            );

        } else if (isDamaged && !isIrreparable) {
            // CASO 2: Daño REPARABLE
            tool.setStatus("En reparación");
            tool.setStock(stockActual + 1);

            double cargoReparacion = configService.getCargoReparacion();
            loan.setFine((loan.getFine() == null ? 0 : loan.getFine()) + cargoReparacion);

            kardexService.registerMovement(
                    tool.getId(),
                    "DEVOLUCION",
                    1,
                    username,
                    String.format("Devolución con daño reparable (préstamo #%d)", loanId),
                    loanId
            );

        } else {
            // CASO 3: Sin daños
            tool.setStatus("Disponible");
            tool.setStock(stockActual + 1);

            kardexService.registerMovement(
                    tool.getId(),
                    "DEVOLUCION",
                    1,
                    username,
                    String.format("Devolución normal (préstamo #%d)", loanId),
                    loanId
            );
        }

        // Establecer estado según multa
        if (loan.getFine() != null && loan.getFine() > 0) {
            loan.setStatus("Atrasado");
        } else {
            loan.setStatus("Devuelto");
        }

        toolRepository.save(tool);
        return loanRepository.save(loan);
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }
}