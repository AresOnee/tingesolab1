package com.example.demo.Controller;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de configuraciones y tarifas del sistema
 * Épica 4: Gestión de montos y tarifas
 *
 * ✅ NUEVA FUNCIONALIDAD: Endpoints para cargo por reparación
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config", description = "Gestión de configuraciones y tarifas del sistema (Épica 4)")
@SecurityRequirement(name = "bearerAuth")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * Extraer username del JWT de Keycloak
     * ✅ CORREGIDO: Maneja múltiples fallbacks para tests y producción
     */
    private String extractUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Intentar obtener "preferred_username" (típico de Keycloak)
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isEmpty()) {
                return preferredUsername;
            }

            // Si no existe, intentar "name"
            String name = jwt.getClaimAsString("name");
            if (name != null && !name.isEmpty()) {
                return name;
            }

            // Si no existe, usar "sub" (subject - UUID)
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.isEmpty()) {
                return sub;
            }
        }

        // Fallback final: usar getName() del authentication (para @WithMockUser en tests)
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        return "UNKNOWN";
    }

    /**
     * RF4.1 y RF4.2: Obtener todas las configuraciones
     * GET /api/v1/config
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
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
    @Operation(summary = "Obtener tarifa de arriendo diaria",
            description = "RF4.1: Retorna el valor configurado de la tarifa diaria de arriendo")
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
    @Operation(summary = "Obtener tarifa de multa diaria",
            description = "RF4.2: Retorna el valor configurado de la tarifa diaria de multa por atraso")
    public ResponseEntity<Map<String, Double>> getTarifaMulta() {
        Double tarifa = configService.getTarifaMultaDiaria();
        return ResponseEntity.ok(Map.of("tarifaMultaDiaria", tarifa));
    }

    /**
     * ✅ NUEVO: Obtener cargo por reparación
     * GET /api/v1/config/cargo-reparacion
     *
     * Épica 2 - RN #16: Retorna el cargo configurable para herramientas con daños leves
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/cargo-reparacion")
    @Operation(summary = "Obtener cargo por reparación",
            description = "Épica 2 - RN #16: Retorna el cargo fijo por reparación de herramientas con daños leves")
    public ResponseEntity<Map<String, Double>> getCargoReparacion() {
        Double cargo = configService.getCargoReparacion();
        return ResponseEntity.ok(Map.of("cargoReparacion", cargo));
    }

    /**
     * RF4.1 y RF4.2: Actualizar configuración por ID (solo Admin)
     * PUT /api/v1/config/{id}
     * Body: { "value": 5000.0 }
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración por ID (solo Admin)",
            description = "RF4.1 y RF4.2: Actualiza el valor de cualquier configuración del sistema")
    public ResponseEntity<ConfigEntity> updateConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = extractUsername(authentication);

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
    @Operation(summary = "Actualizar tarifa de arriendo (solo Admin)",
            description = "RF4.1: Actualiza la tarifa diaria de arriendo de herramientas")
    public ResponseEntity<ConfigEntity> updateTarifaArriendo(
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = extractUsername(authentication);

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
    @Operation(summary = "Actualizar tarifa de multa (solo Admin)",
            description = "RF4.2: Actualiza la tarifa diaria de multa por atraso")
    public ResponseEntity<ConfigEntity> updateTarifaMulta(
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = extractUsername(authentication);

        ConfigEntity updated = configService.setTarifaMultaDiaria(newValue, username);
        return ResponseEntity.ok(updated);
    }

    /**
     * ✅ NUEVO: Actualizar cargo por reparación (solo Admin)
     * PUT /api/v1/config/cargo-reparacion
     * Body: { "value": 10000.0 }
     *
     * Épica 2 - RN #16: Permite configurar el cargo por reparación de daños leves
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/cargo-reparacion")
    @Operation(summary = "Actualizar cargo por reparación (solo Admin)",
            description = "Épica 2 - RN #16: Actualiza el cargo fijo por reparación de herramientas con daños leves")
    public ResponseEntity<ConfigEntity> updateCargoReparacion(
            @RequestBody Map<String, Double> body,
            Authentication authentication
    ) {
        Double newValue = body.get("value");
        String username = extractUsername(authentication);

        ConfigEntity updated = configService.setCargoReparacion(newValue, username);
        return ResponseEntity.ok(updated);
    }
}