package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanRepository loanRepository;

    // ==================== MÉTODOS CRUD (PARA TESTS) ====================

    /**
     * Crear un nuevo cliente
     * Validaciones:
     * - RUT no puede estar duplicado
     * - Email no puede estar duplicado
     * - Si state es null, asigna "Activo" por defecto
     */
    @Transactional
    public ClientEntity create(ClientEntity client) {
        // Validar RUT duplicado
        if (clientRepository.existsByRut(client.getRut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RUT ya existe");
        }

        // Validar email duplicado
        if (clientRepository.existsByEmail(client.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya existe");
        }

        // Asignar estado por defecto si es null
        if (client.getState() == null || client.getState().isBlank()) {
            client.setState("Activo");
        }

        return clientRepository.save(client);
    }

    /**
     * Obtener todos los clientes
     */
    public List<ClientEntity> getAll() {
        return clientRepository.findAll();
    }

    /**
     * Obtener cliente por ID
     * Lanza 404 si no existe
     */
    public ClientEntity getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Cliente con ID %d no encontrado", id)
                ));
    }

    /**
     * Actualizar un cliente existente
     * Validaciones:
     * - NO permite cambiar RUT
     * - NO permite cambiar State (usar updateState)
     * - Email no puede estar duplicado
     */
    @Transactional
    public ClientEntity update(Long id, ClientEntity updates) {
        ClientEntity existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Cliente con ID %d no encontrado", id)
                ));

        // Actualizar solo campos permitidos
        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }

        if (updates.getPhone() != null) {
            existing.setPhone(updates.getPhone());
        }

        if (updates.getEmail() != null && !updates.getEmail().equals(existing.getEmail())) {
            // Validar email duplicado
            if (clientRepository.existsByEmail(updates.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya existe");
            }
            existing.setEmail(updates.getEmail());
        }

        // NO cambiar RUT ni State en update

        return clientRepository.save(existing);
    }

    /**
     * Actualizar el estado de un cliente manualmente
     * Estados válidos: "Activo", "Restringido"
     */
    @Transactional
    public ClientEntity updateState(Long id, String newState) {
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format("Cliente con ID %d no encontrado", id)
                ));

        // Validar estado
        if (!"Activo".equals(newState) && !"Restringido".equals(newState)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado inválido. Valores permitidos: Activo, Restringido"
            );
        }

        client.setState(newState);
        return clientRepository.save(client);
    }

    /**
     * Eliminar un cliente
     * Lanza 404 si no existe
     */
    @Transactional
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Cliente con ID %d no encontrado", id)
            );
        }
        clientRepository.deleteById(id);
    }

    // ==================== MÉTODOS LEGACY (COMPATIBILIDAD CON CONTROLLER) ====================

    /**
     * Obtener todos los clientes (nombre legacy)
     */
    public List<ClientEntity> getAllClients() {
        return getAll();
    }

    /**
     * Obtener cliente por ID (nombre legacy, retorna null si no existe)
     */
    public ClientEntity getClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    /**
     * Crear o actualizar cliente (nombre legacy)
     */
    @Transactional
    public ClientEntity saveClient(ClientEntity client) {
        return clientRepository.save(client);
    }

    // ==================== RF3.2: ACTUALIZACIÓN AUTOMÁTICA DE ESTADOS ====================

    /**
     * RF3.2: Cambiar estado de cliente a "Restringido" en caso de atrasos
     *
     * Este método verifica si un cliente tiene préstamos atrasados
     * y actualiza su estado automáticamente.
     *
     * @param clientId ID del cliente a verificar
     * @return true si se actualizó el estado, false si no
     */
    @Transactional
    public boolean updateClientStateBasedOnLoans(Long clientId) {
        ClientEntity client = clientRepository.findById(clientId).orElse(null);
        if (client == null) {
            return false;
        }

        // Verificar si tiene préstamos atrasados o multas pendientes
        boolean hasProblems = loanRepository.hasOverduesOrFines(clientId);

        if (hasProblems && "Activo".equalsIgnoreCase(client.getState())) {
            // ✅ RF3.2: Cambiar a Restringido (sin validaciones)
            clientRepository.updateClientState(clientId, "Restringido");
            return true;
        } else if (!hasProblems && "Restringido".equalsIgnoreCase(client.getState())) {
            // ✅ Restaurar a Activo (sin validaciones)
            clientRepository.updateClientState(clientId, "Activo");
            return true;
        }

        return false;
    }

    /**
     * RF3.2: Actualizar el estado de TODOS los clientes
     * según sus préstamos activos.
     *
     * Este método debe ejecutarse periódicamente o después
     * de operaciones críticas (devoluciones, creación de préstamos, etc.)
     */
    @Transactional
    public void updateAllClientStates() {
        List<ClientEntity> allClients = clientRepository.findAll();

        for (ClientEntity client : allClients) {
            boolean hasProblems = loanRepository.hasOverduesOrFines(client.getId());

            if (hasProblems && "Activo".equalsIgnoreCase(client.getState())) {
                clientRepository.updateClientState(client.getId(), "Restringido");
            } else if (!hasProblems && "Restringido".equalsIgnoreCase(client.getState())) {
                clientRepository.updateClientState(client.getId(), "Activo");
            }
        }
    }

    /**
     * ⏰ Tarea programada: Actualizar estados de clientes diariamente a las 00:01
     *
     * Este método se ejecuta automáticamente todos los días a las 00:01 AM
     * para mantener los estados de los clientes actualizados según sus préstamos.
     *
     * Cron: "0 1 0 * * *"
     *   - 0 = segundo 0
     *   - 1 = minuto 1
     *   - 0 = hora 0 (medianoche)
     *   - * = todos los días del mes
     *   - * = todos los meses
     *   - * = todos los días de la semana
     */
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void scheduledUpdateClientStates() {
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("⏰ [" + timestamp + "] Actualización automática diaria");
        System.out.println("═══════════════════════════════════════════");

        try {
            updateAllClientStates();
            System.out.println("✅ Estados de clientes actualizados correctamente");
        } catch (Exception e) {
            System.err.println("❌ Error al actualizar estados: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("═══════════════════════════════════════════\n");
    }
}