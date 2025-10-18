package com.example.demo.Service;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ToolRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ToolService {

    @Autowired private ToolRepository toolRepository;
    @Autowired private KardexService kardexService;

    public List<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }

    public ToolEntity saveTool(ToolEntity tool) {
        if (tool.getStatus() == null || tool.getStatus().isBlank()) {
            tool.setStatus("Disponible");
        }
        return toolRepository.save(tool);
    }

    /**
     * RF1.1: Registrar nuevas herramientas
     * RF5.1: Registrar automáticamente en kardex
     * ✅ CORREGIDO: Ahora recibe username del usuario autenticado
     */
    public ToolEntity create(ToolEntity body, String username) {  // ← Agregado parámetro username
        String name = Optional.ofNullable(body.getName()).orElse("").trim();
        if (name.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (toolRepository.existsByNameIgnoreCase(name)) {
            throw new DataIntegrityViolationException("uq_tools_name");
        }
        body.setName(name);
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("Disponible");
        }

        // Guardar herramienta
        ToolEntity saved = toolRepository.save(body);

        // ✅ RF5.1: Registrar movimiento en kardex con username real
        kardexService.registerMovement(
                saved.getId(),
                "REGISTRO",
                saved.getStock(),
                username,  // ← Ahora usa el username real de Keycloak (ej: "diego")
                "Alta de herramienta: " + saved.getName(),
                null
        );

        return saved;
    }

    /**
     * RF1.2: Dar de baja herramientas dañadas o en desuso (solo Administrador)
     * RF5.1: Registrar automáticamente en kardex
     * ✅ CORREGIDO: Ahora recibe username del usuario autenticado
     */
    public ToolEntity decommission(Long toolId, String username) {  // ← Agregado parámetro username
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herramienta no encontrada"));

        // Validar que la herramienta no esté prestada
        if ("Prestada".equalsIgnoreCase(tool.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede dar de baja una herramienta que está prestada");
        }

        // Validar que no esté ya dada de baja
        if ("Dada de baja".equalsIgnoreCase(tool.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La herramienta ya está dada de baja");
        }

        // Guardar stock anterior para el kardex
        int stockAnterior = tool.getStock();

        // Dar de baja: cambiar estado y poner stock en 0
        tool.setStatus("Dada de baja");
        tool.setStock(0);
        ToolEntity saved = toolRepository.save(tool);

        // ✅ RF5.1: Registrar baja en kardex con username real
        kardexService.registerMovement(
                saved.getId(),
                "BAJA",
                -stockAnterior, // negativo porque se reduce
                username,  // ← Ahora usa el username real de Keycloak (ej: "diego")
                "Baja de herramienta: " + saved.getName(),
                null
        );

        return saved;
    }
}