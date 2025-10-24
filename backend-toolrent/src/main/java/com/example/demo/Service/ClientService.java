package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
            // F3.2: Cambiar a Restringido
            client.setState("Restringido");
            clientRepository.save(client);
            return true;
        } else if (!hasProblems && "Restringido".equalsIgnoreCase(client.getState())) {
            // Restaurar a Activo si ya no tiene problemas
            client.setState("Activo");
            clientRepository.save(client);
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
        LocalDate today = LocalDate.now();

        for (ClientEntity client : allClients) {
            // Obtener todos los préstamos del cliente
            List<LoanEntity> clientLoans = loanRepository.findAll().stream()
                    .filter(loan -> loan.getClient().getId().equals(client.getId()))
                    .toList();

            // Verificar si tiene problemas
            boolean hasOverdueLoans = clientLoans.stream()
                    .anyMatch(loan ->
                            loan.getReturnDate() == null &&
                                    loan.getDueDate().isBefore(today)
                    );

            boolean hasPendingFines = clientLoans.stream()
                    .anyMatch(loan ->
                            loan.getFine() != null &&
                                    loan.getFine() > 0 &&
                                    "Atrasado".equals(loan.getStatus())
                    );

            boolean hasProblems = hasOverdueLoans || hasPendingFines;

            // Actualizar estado según corresponda
            if (hasProblems && !"Restringido".equalsIgnoreCase(client.getState())) {
                client.setState("Restringido");
                clientRepository.save(client);
            } else if (!hasProblems && "Restringido".equalsIgnoreCase(client.getState())) {
                client.setState("Activo");
                clientRepository.save(client);
            }
        }
    }

    /**
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
    @Scheduled(cron = "0 1 0 * * *")
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
    // ========== FIN NUEVO MÉTODO ==========

    /**
     * Obtener todos los clientes
     */
    public List<ClientEntity> getAllClients() {
        return clientRepository.findAll();
    }

    /**
     * Obtener cliente por ID
     */
    public ClientEntity getClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }

    /**
     * Crear o actualizar cliente
     */
    @Transactional
    public ClientEntity saveClient(ClientEntity client) {
        return clientRepository.save(client);
    }
}