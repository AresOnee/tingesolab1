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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @Autowired private ClientService clientService;

    /**
     * RF2.1: Crea un préstamo aplicando reglas de negocio
     * RF3.2: Actualiza estado del cliente automáticamente
     * RF5.1: Registrar automáticamente en kardex
     */
    @Transactional
    public LoanEntity createLoan(Long clientId, Long toolId, LocalDate dueDate, String username) {
        // ✅ Actualizar estado del cliente ANTES de validar
        clientService.updateClientStateBasedOnLoans(clientId);

        // Validaciones básicas
        if (clientId == null || toolId == null || dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los campos cliente, herramienta y fecha de devolución son obligatorios");
        }

        // Cargar entidades
        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Cliente con ID %d no encontrado", clientId)));

        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Herramienta con ID %d no encontrada", toolId)));

        // Validaciones de negocio
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
            List<LoanEntity> problematicLoans = allLoans.stream()
                    .filter(l -> Objects.equals(l.getClient().getId(), clientId))
                    .filter(l ->
                            (l.getReturnDate() == null && l.getDueDate().isBefore(today)) ||
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

        // ✅ VALIDACIÓN 4 (MODIFICADA): Stock >= 1
        int stockTotal = tool.getStock();
        if (stockTotal < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("La herramienta '%s' no tiene stock disponible (Stock total: %d)",
                            tool.getName(), stockTotal));
        }

        // ✅ VALIDACIÓN 4b (NUEVA): Verificar unidades realmente disponibles
        long unidadesPrestadas = countActiveLoansForTool(toolId);
        long unidadesDisponibles = stockTotal - unidadesPrestadas;

        if (unidadesDisponibles < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("La herramienta '%s' no tiene unidades disponibles para prestar.\n" +
                                    "Stock total: %d | Prestadas actualmente: %d | Disponibles: %d",
                            tool.getName(), stockTotal, unidadesPrestadas, unidadesDisponibles));
        }

        // ✅ VALIDACIÓN 5: Máximo 5 préstamos activos
        long activos = allLoans.stream()
                .filter(l -> Objects.equals(l.getClient().getId(), clientId) && l.getReturnDate() == null)
                .count();
        if (activos >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("El cliente '%s' ya tiene 5 préstamos activos. " +
                                    "Debe devolver al menos una herramienta antes de solicitar un nuevo préstamo.",
                            client.getName()));
        }

        // ✅ VALIDACIÓN 6: No puede tener la misma herramienta activa
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

        // Calcular costo de arriendo
        double tarifaDiaria = configService.getTarifaArriendoDiaria();
        long dias = ChronoUnit.DAYS.between(today, dueDate);
        if (dias < 1) dias = 1;
        double costoArriendo = dias * tarifaDiaria;

        // Crear préstamo
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

        // ✅ CAMBIO CLAVE: NO modificar stock
        // El stock representa el total de unidades, no las disponibles

        LoanEntity saved = loanRepository.save(loan);

        // ✅ Actualizar estado de herramienta dinámicamente
        updateToolStatus(tool);
        toolRepository.save(tool);

        // Registrar en kardex
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
     * RF3.2: Actualizar estado del cliente automáticamente
     * RF5.1: Registrar automáticamente en kardex
     */
    @Transactional
    public LoanEntity returnTool(Long loanId, boolean isDamaged, boolean isIrreparable, String username) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Préstamo con ID %d no encontrado", loanId)));
        var tool = loan.getTool();

        if ("Devuelto".equalsIgnoreCase(loan.getStatus())) {
            return loan;
        }

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);

        // Calcular multa por atraso
        long daysLate = Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate(), today));
        double fineDaily = configService.getTarifaMultaDiaria();

        if (daysLate > 0) {
            loan.setFine((loan.getFine() == null ? 0 : loan.getFine()) + (daysLate * fineDaily));
        }

        loan.setDamaged(isDamaged);
        loan.setIrreparable(isIrreparable);

        // Procesar según tipo de daño
        if (isDamaged && isIrreparable) {
            // ✅ CAMBIO: NO modificar stock ni estado manualmente
            // Daño irreparable: cobrar valor de reposición
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
            // ✅ CAMBIO: NO modificar stock ni estado manualmente
            // Daño reparable: aplicar cargo de reparación
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
            // ✅ CAMBIO: NO modificar stock ni estado manualmente
            // Sin daño: devolución normal
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

        LoanEntity savedLoan = loanRepository.save(loan);

        // ✅ CLAVE: Actualizar estado de herramienta dinámicamente
        updateToolStatus(tool);
        toolRepository.save(tool);

        // ✅ Actualizar estado del cliente después de la devolución
        clientService.updateClientStateBasedOnLoans(loan.getClient().getId());

        return savedLoan;
    }

    public List<LoanEntity> getAllLoans() {
        return loanRepository.findAll();
    }

    // ========================================
    // ✅ NUEVOS MÉTODOS: GESTIÓN DE ESTADO DINÁMICO
    // ========================================

    /**
     * ✅ NUEVO: Contar préstamos activos de una herramienta específica
     *
     * Un préstamo está activo si no tiene fecha de devolución (returnDate == null)
     *
     * @param toolId ID de la herramienta
     * @return Número de préstamos activos
     */
    private long countActiveLoansForTool(Long toolId) {
        return loanRepository.findAll().stream()
                .filter(l -> Objects.equals(l.getTool().getId(), toolId))
                .filter(l -> l.getReturnDate() == null) // Préstamos activos (no devueltos)
                .count();
    }

    /**
     * ✅ NUEVO: Actualizar estado de herramienta basado en disponibilidad real
     *
     * Lógica de estado:
     * 1. Si hay unidades disponibles (stock - préstamos activos > 0) → "Disponible"
     * 2. Si no hay disponibles pero hay en reparación → "En reparación"
     * 3. Si todas están prestadas → "Prestada"
     * 4. Si todas están dadas de baja → "Dada de baja"
     * 5. Si no hay stock → "Sin stock"
     *
     * @param tool Herramienta a actualizar
     */
    private void updateToolStatus(ToolEntity tool) {
        int stockTotal = tool.getStock();

        if (stockTotal == 0) {
            tool.setStatus("Sin stock");
            return;
        }

        // Contar préstamos activos (no devueltos)
        long prestamosActivos = countActiveLoansForTool(tool.getId());

        // Calcular unidades realmente disponibles
        long unidadesDisponibles = stockTotal - prestamosActivos;

        // Contar unidades en reparación (devueltas con daño reparable)
        long enReparacion = loanRepository.findAll().stream()
                .filter(l -> Objects.equals(l.getTool().getId(), tool.getId()))
                .filter(l -> l.getReturnDate() != null) // Ya devueltas
                .filter(l -> Boolean.TRUE.equals(l.getDamaged())) // Con daño
                .filter(l -> Boolean.FALSE.equals(l.getIrreparable())) // Reparable
                .count();

        // Contar unidades dadas de baja (devueltas con daño irreparable)
        long dadasDeBaja = loanRepository.findAll().stream()
                .filter(l -> Objects.equals(l.getTool().getId(), tool.getId()))
                .filter(l -> l.getReturnDate() != null) // Ya devueltas
                .filter(l -> Boolean.TRUE.equals(l.getIrreparable())) // Irreparable
                .count();

        // Determinar estado según prioridad
        if (dadasDeBaja >= stockTotal) {
            // Todas las unidades están dadas de baja
            tool.setStatus("Dada de baja");
        } else if (unidadesDisponibles > 0) {
            // ✅ CLAVE: Hay unidades disponibles para prestar
            tool.setStatus("Disponible");
        } else if (enReparacion > 0) {
            // Todas están prestadas o en reparación
            tool.setStatus("En reparación");
        } else {
            // Todas están prestadas
            tool.setStatus("Prestada");
        }
    }

    // ========================================
    // ✅ MÉTODOS PARA ACTUALIZACIÓN AUTOMÁTICA
    // ========================================

    /**
     * ⏰ Tarea programada: Actualizar estados de préstamos
     *
     * 🧪 TESTING: Se ejecuta cada minuto para pruebas
     * 📅 PRODUCCIÓN: Cambiar a "0 1 0 * * *" para ejecutar a las 00:01
     */
    @Scheduled(cron = "0 */1 * * * *")  // 🧪 CADA MINUTO (TESTING)
    // @Scheduled(cron = "0 1 0 * * *")  // 📅 DIARIO 00:01 (PRODUCCIÓN) - Descomentar después
    @Transactional
    public void scheduledUpdateLoanStatuses() {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("⏰ [" + timestamp + "] Actualización de préstamos");
        System.out.println("═══════════════════════════════════════════");

        try {
            int updated = updateOverdueLoans();
            System.out.println("✅ " + updated + " préstamo(s) marcado(s) como Atrasado");

            // Actualizar estados de clientes
            clientService.updateAllClientStates();
            System.out.println("✅ Estados de clientes actualizados");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("═══════════════════════════════════════════\n");
    }

    /**
     * Actualizar préstamos vencidos de "Vigente" a "Atrasado"
     *
     * @return Número de préstamos actualizados
     */
    @Transactional
    public int updateOverdueLoans() {
        LocalDate today = LocalDate.now();

        // Buscar préstamos vigentes con fecha vencida
        List<LoanEntity> overdueLoans = loanRepository.findAll().stream()
                .filter(loan ->
                        "Vigente".equalsIgnoreCase(loan.getStatus()) &&
                                loan.getReturnDate() == null &&
                                loan.getDueDate().isBefore(today)
                )
                .toList();

        int count = 0;
        for (LoanEntity loan : overdueLoans) {
            // Calcular multa
            long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            double fineDaily = configService.getTarifaMultaDiaria();
            double totalFine = daysLate * fineDaily;

            // Actualizar préstamo
            loan.setStatus("Atrasado");
            loan.setFine(totalFine);
            loanRepository.save(loan);

            count++;

            System.out.println(String.format(
                    "  🔴 Préstamo #%d (%s - %s): Vigente → Atrasado (%d día(s), multa: $%.0f)",
                    loan.getId(),
                    loan.getClient().getName(),
                    loan.getTool().getName(),
                    daysLate,
                    totalFine
            ));
        }

        return count;
    }
}