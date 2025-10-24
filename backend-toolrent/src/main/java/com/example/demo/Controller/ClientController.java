package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    /**
     * Obtener todos los clientes
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping({ "", "/" })
    public ResponseEntity<List<ClientEntity>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    /**
     * Obtener cliente por ID
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
        ClientEntity client = clientService.getClientById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    /**
     * Crear cliente
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({ "", "/" })
    public ResponseEntity<ClientEntity> createOrUpdateClient(@RequestBody ClientEntity client) {
        ClientEntity saved = clientService.saveClient(client);
        return ResponseEntity.ok(saved);
    }

    /**
     * Actualizar cliente (permite cambio manual de estado por admin)
     *
     * PUT /api/v1/clients/{id}
     * Body: { "state": "Restringido" }  o  { "state": "Activo" }
     *
     * También permite actualizar otros campos: name, rut, email, phone
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ClientEntity> updateClient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        ClientEntity client = clientService.getClientById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        // Actualizar solo los campos que vienen en el body
        if (updates.containsKey("state")) {
            String newState = (String) updates.get("state");

            // Validar estados permitidos
            if (!"Activo".equals(newState) && !"Restringido".equals(newState)) {
                throw new IllegalArgumentException(
                        "Estado inválido. Solo se permiten: 'Activo' o 'Restringido'"
                );
            }

            client.setState(newState);
        }

        // Actualizar otros campos si vienen
        if (updates.containsKey("name")) {
            client.setName((String) updates.get("name"));
        }
        if (updates.containsKey("rut")) {
            client.setRut((String) updates.get("rut"));
        }
        if (updates.containsKey("email")) {
            client.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("phone")) {
            client.setPhone((String) updates.get("phone"));
        }

        ClientEntity saved = clientService.saveClient(client);
        return ResponseEntity.ok(saved);
    }

    /**
     * RF3.2: Actualizar el estado de UN cliente específico
     * basado en sus préstamos activos (automático según reglas)
     *
     * PUT /api/v1/clients/{id}/update-state
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/update-state")
    public ResponseEntity<Map<String, Object>> updateClientState(@PathVariable Long id) {
        boolean updated = clientService.updateClientStateBasedOnLoans(id);

        ClientEntity client = clientService.getClientById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("updated", updated);
        response.put("clientId", id);
        response.put("currentState", client != null ? client.getState() : null);

        return ResponseEntity.ok(response);
    }

    /**
     * RF3.2: Actualizar el estado de TODOS los clientes
     * basado en sus préstamos activos
     *
     * POST /api/v1/clients/update-all-states
     *
     * Este endpoint debe ser ejecutado:
     * - Manualmente por un administrador
     * - Automáticamente por un cron job (cada hora, diariamente, etc.)
     * - Después de operaciones críticas (devoluciones, multas, etc.)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/update-all-states")
    public ResponseEntity<Map<String, String>> updateAllClientStates() {
        clientService.updateAllClientStates();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Estados de clientes actualizados correctamente según sus préstamos");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}