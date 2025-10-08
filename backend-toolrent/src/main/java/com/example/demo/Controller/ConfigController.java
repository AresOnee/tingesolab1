package com.example.demo.Controller;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestionar configuraciones y tarifas del sistema
 * Épica 4: Gestión de montos y tarifas
 * Solo accesible para Administradores
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * RF4.1 y RF4.2: Obtener todas las configuraciones
     * GET /api/v1/config
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping({"", "/"})
    public ResponseEntity<List<ConfigEntity>> getAllConfigs() {
        List<ConfigEntity> configs = configService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * RF4.1 y RF4.2: Obtener configuración por clave
     * GET /api/v1/config/key/{configKey}
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/key/{configKey}")
    public ResponseEntity<ConfigEntity> getConfigByKey(@PathVariable String configKey) {
        ConfigEntity config = configService.getConfigByKey(configKey);
        return ResponseEntity.ok(config);
    }

    /**
     * RF4.1: Obtener tarifa de arriendo diaria
     * GET /api/v1/config/tarifa-arriendo
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/tarifa-arriendo")
    public ResponseEntity<Map<String, Double>> getTarifaArriendo() {
        Double tarifa = configService.getTarifaArriendoDiaria();
        return ResponseEntity.ok(Map.of("tarifaArriendoDiaria", tarifa));
    }

    /**
     * RF4.2: Obtener tarifa de multa diaria
     * GET /api/v1/config/tarifa-multa
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/tarifa-multa")
    public ResponseEntity<Map<String, Double>> getTarifaMulta() {
        Double tarifa = configService.getTarifaMultaDiaria();
        return ResponseEntity.ok(Map.of("tarifaMultaDiaria", tarifa));
    }

    /**
     * RF4.1 y RF4.2: Actualizar configuración por ID (solo Admin)
     * PUT /api/v1/config/{id}
     * Body: { "value": 5000.0 }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ConfigEntity> updateConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = authentication.getName();

        ConfigEntity updated = configService.updateConfigById(id, newValue, username);
        return ResponseEntity.ok(updated);
    }

    /**
     * RF4.1: Actualizar tarifa de arriendo (solo Admin)
     * PUT /api/v1/config/tarifa-arriendo
     * Body: { "value": 5000.0 }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/tarifa-arriendo")
    public ResponseEntity<ConfigEntity> updateTarifaArriendo(
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = authentication.getName();

        ConfigEntity updated = configService.setTarifaArriendoDiaria(newValue, username);
        return ResponseEntity.ok(updated);
    }

    /**
     * RF4.2: Actualizar tarifa de multa (solo Admin)
     * PUT /api/v1/config/tarifa-multa
     * Body: { "value": 2000.0 }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/tarifa-multa")
    public ResponseEntity<ConfigEntity> updateTarifaMulta(
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = authentication.getName();

        ConfigEntity updated = configService.setTarifaMultaDiaria(newValue, username);
        return ResponseEntity.ok(updated);
    }
}