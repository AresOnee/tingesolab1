package com.example.demo.Controller;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de clientes (Gap Analysis - Puntos 8 y 9)
 *
 * ✅ ENDPOINTS CRUD COMPLETOS:
 * - GET    /api/v1/clients/       → Listar todos
 * - GET    /api/v1/clients/{id}   → Obtener por ID
 * - POST   /api/v1/clients        → Crear cliente
 * - PUT    /api/v1/clients/{id}   → Actualizar cliente
 * - PATCH  /api/v1/clients/{id}/state → Cambiar estado
 * - DELETE /api/v1/clients/{id}   → Eliminar cliente (opcional)
 *
 * ✅ SEGURIDAD:
 * - Listar/Ver: USER o ADMIN
 * - Crear/Actualizar/Eliminar: Solo ADMIN
 */
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    /**
     * GET /api/v1/clients/
     * Listar todos los clientes
     *
     * Acceso: USER o ADMIN
     *
     * @return Lista de clientes
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/")
    public List<ClientEntity> getAll() {
        return clientService.getAll();
    }

    /*
     * GET /api/v1/clients/{id}
     * Obtener un cliente específico por ID
     *
     * Acceso: USER o ADMIN
     *
     * @param id ID del cliente
     * @return Cliente encontrado
     * @throws ResponseStatusException 404 si no existe
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ClientEntity getById(@PathVariable Long id) {
        return clientService.getById(id);
    }

    /**
     * POST /api/v1/clients
     * Crear un nuevo cliente
     *
     * Acceso: Solo ADMIN
     *
     * Validaciones automáticas (@Valid):
     * - Nombre: obligatorio, 2-100 caracteres
     * - RUT: obligatorio, formato XX.XXX.XXX-X
     * - Email: obligatorio, formato válido
     * - Teléfono: obligatorio, formato +56XXXXXXXXX
     * - Estado: opcional, por defecto "Activo"
     *
     * Validaciones de negocio:
     * - RUT único
     * - Email único
     *
     * @param body Datos del cliente a crear
     * @return 201 Created con el cliente creado y header Location

     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ClientEntity> create(@Valid @RequestBody ClientEntity body) {
        ClientEntity saved = clientService.create(body);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    /**
     * PUT /api/v1/clients/{id}
     * Actualizar datos completos de un cliente
     *
     * Acceso: Solo ADMIN
     *
     * Campos que se pueden actualizar:
     * - name: Nombre del cliente
     * - phone: Teléfono
     * - email: Email (debe ser único)
     *
     * Campos que NO se pueden cambiar:
     * - rut: Es inmutable
     * - state: Usar PATCH /clients/{id}/state
     *
     * @param id ID del cliente a actualizar

     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ClientEntity> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientEntity body) {

        ClientEntity updated = clientService.update(id, body);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/v1/clients/{id}/state
     * Cambiar el estado de un cliente (Activo ↔ Restringido)
     *
     * Acceso: Solo ADMIN
     *
     * RF3.2: Cambiar estado de cliente a "restringido" en caso de atrasos
     *
     * Estados válidos:
     * - "Activo": Puede solicitar préstamos
     * - "Restringido": No puede solicitar préstamos hasta regularizar
     *
     * Body esperado:
     * {
     *   "state": "Restringido"
     * }
     *
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/state")
    public ResponseEntity<ClientEntity> updateState(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newState = body.get("state");

        if (newState == null || newState.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El campo 'state' es obligatorio");
        }

        ClientEntity updated = clientService.updateState(id, newState);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/clients/{id}
     * Eliminar un cliente (opcional - para CRUD completo)
     *
     * Acceso: Solo ADMIN
     *
     * NOTA: En producción, evaluar si es mejor hacer borrado lógico
     * (cambiar estado a "Inactivo") en lugar de borrado físico
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}