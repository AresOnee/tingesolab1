package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de gestión de clientes (Gap Analysis - Puntos 8 y 9)
 *
 * ✅ OPERACIONES CRUD COMPLETAS:
 * - CREATE: Crear cliente con validaciones
 * - READ: Listar todos los clientes
 * - UPDATE: Actualizar datos del cliente
 * - UPDATE STATE: Cambiar estado (Activo ↔ Restringido)
 *
 * ✅ VALIDACIONES:
 * - RUT único
 * - Email único
 * - Estado por defecto "Activo"
 * - Formatos validados por Bean Validation
 */
@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    /**
     * RF3.1: Obtener todos los clientes
     */
    public List<ClientEntity> getAll() {
        return clientRepository.findAll();
    }

    /**
     * RF3.1: Crear un nuevo cliente
     *
     * Validaciones:
     * - RUT debe ser único
     * - Email debe ser único
     * - Estado por defecto: "Activo"
     * - Formato de campos validado por @Valid en el controller
     *
     * @param body Datos del cliente a crear
     * @return Cliente creado con ID asignado
     * @throws ResponseStatusException 409 si RUT o email ya existen
     */
    @Transactional
    public ClientEntity create(ClientEntity body) {
        // Validación: RUT único
        if (clientRepository.existsByRut(body.getRut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El RUT ya existe en el sistema");
        }

        // Validación: Email único
        if (clientRepository.existsByEmail(body.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El email ya existe en el sistema");
        }

        // Asignar estado por defecto si no viene o está vacío
        if (body.getState() == null || body.getState().isBlank()) {
            body.setState("Activo");
        }

        return clientRepository.save(body);
    }

    /**
     * RF3.1: Actualizar datos completos de un cliente
     *
     * Permite actualizar: name, phone, email (NO permite cambiar RUT ni state)
     * Para cambiar el estado usar el método updateState()
     *
     * Validaciones:
     * - Cliente debe existir
     * - Email debe ser único (si se cambia)
     * - No permite cambiar el RUT (inmutable)
     * - No permite cambiar el estado (usar updateState)
     *
     * @param id ID del cliente a actualizar
     * @param body Nuevos datos del cliente
     * @return Cliente actualizado
     * @throws ResponseStatusException 404 si el cliente no existe
     * @throws ResponseStatusException 409 si el nuevo email ya existe
     */
    @Transactional
    public ClientEntity update(Long id, ClientEntity body) {
        // Verificar que el cliente existe
        ClientEntity existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + id));

        // Validar que el email sea único (si se está cambiando)
        if (!existing.getEmail().equals(body.getEmail())) {
            if (clientRepository.existsByEmail(body.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El email ya existe en el sistema");
            }
        }

        // Actualizar solo los campos permitidos
        existing.setName(body.getName());
        existing.setPhone(body.getPhone());
        existing.setEmail(body.getEmail());

        // NO permitir cambiar RUT (es inmutable)
        // NO permitir cambiar state (usar updateState para eso)

        return clientRepository.save(existing);
    }

    /**
     * RF3.2: Cambiar estado de un cliente (Activo ↔ Restringido)
     *
     * Este método es específico para cambiar el estado del cliente
     * según las reglas de negocio (por ejemplo, cuando tiene deudas)
     *
     * Estados válidos:
     * - "Activo": Puede solicitar préstamos
     * - "Restringido": No puede solicitar préstamos
     *
     * @param id ID del cliente
     * @param newState Nuevo estado ("Activo" o "Restringido")
     * @return Cliente con estado actualizado
     * @throws ResponseStatusException 404 si el cliente no existe
     * @throws ResponseStatusException 400 si el estado es inválido
     */
    @Transactional
    public ClientEntity updateState(Long id, String newState) {
        // Verificar que el cliente existe
        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + id));

        // Validar que el estado sea válido
        if (!"Activo".equalsIgnoreCase(newState) &&
                !"Restringido".equalsIgnoreCase(newState)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estado inválido. Valores permitidos: Activo, Restringido");
        }

        // Actualizar estado
        client.setState(newState);

        return clientRepository.save(client);
    }

    /**
     * Obtener un cliente por ID
     *
     * @param id ID del cliente
     * @return Cliente encontrado
     * @throws ResponseStatusException 404 si el cliente no existe
     */
    public ClientEntity getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado con ID: " + id));
    }

    /**
     * Eliminar un cliente (opcional - para CRUD completo)
     *
     * NOTA: En producción, evaluar si es mejor hacer borrado lógico
     * (cambiar estado a "Inactivo") en lugar de borrado físico
     *
     * @param id ID del cliente a eliminar
     * @throws ResponseStatusException 404 si el cliente no existe
     */
    @Transactional
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cliente no encontrado con ID: " + id);
        }

        clientRepository.deleteById(id);
    }
}