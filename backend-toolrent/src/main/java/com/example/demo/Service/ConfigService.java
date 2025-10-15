package com.example.demo.Service;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Repository.ConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar configuraciones y tarifas del sistema
 * Épica 4: Gestión de montos y tarifas
 *
 * ✅ NUEVA FUNCIONALIDAD: Cargo por reparación configurable (Épica 2 - RN #16)
 */
@Service
@Transactional
public class ConfigService {

    @Autowired
    private ConfigRepository configRepository;

    // Claves de configuración del sistema
    public static final String TARIFA_ARRIENDO_DIARIA = "TARIFA_ARRIENDO_DIARIA";
    public static final String TARIFA_MULTA_DIARIA = "TARIFA_MULTA_DIARIA";

    // ✅ NUEVO: Cargo por reparación de herramientas con daños leves
    public static final String CARGO_REPARACION = "CARGO_REPARACION";

    /**
     * Inicializar valores por defecto si no existen
     *
     * ✅ MODIFICADO: Se agregó inicialización del cargo por reparación
     */
    @PostConstruct
    public void initializeDefaultConfigs() {
        initConfigIfNotExists(TARIFA_ARRIENDO_DIARIA, 5000.0, "Tarifa diaria de arriendo por herramienta");
        initConfigIfNotExists(TARIFA_MULTA_DIARIA, 2000.0, "Tarifa diaria de multa por atraso");

        // ✅ NUEVO: Inicializar cargo por reparación con valor por defecto de $10,000
        initConfigIfNotExists(CARGO_REPARACION, 10000.0,
                "Cargo fijo por reparación de herramientas con daños leves");
    }

    private void initConfigIfNotExists(String key, Double defaultValue, String description) {
        if (!configRepository.existsByConfigKey(key)) {
            ConfigEntity config = new ConfigEntity();
            config.setConfigKey(key);
            config.setConfigValue(defaultValue);
            config.setDescription(description);
            config.setLastModified(LocalDateTime.now());
            config.setModifiedBy("SYSTEM");
            configRepository.save(config);
        }
    }

    /**
     * RF4.1: Obtener todas las configuraciones
     */
    public List<ConfigEntity> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * RF4.1: Obtener configuración por clave
     */
    public ConfigEntity getConfigByKey(String key) {
        return configRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Configuración no encontrada: " + key
                ));
    }

    /**
     * RF4.1: Obtener valor de tarifa de arriendo diaria
     */
    public Double getTarifaArriendoDiaria() {
        return getConfigByKey(TARIFA_ARRIENDO_DIARIA).getConfigValue();
    }

    /**
     * RF4.2: Obtener valor de tarifa de multa diaria
     */
    public Double getTarifaMultaDiaria() {
        return getConfigByKey(TARIFA_MULTA_DIARIA).getConfigValue();
    }

    /**
     * ✅ NUEVO: Obtener valor de cargo por reparación
     * Épica 2 - RN #16: Cargo configurable para herramientas con daños leves
     */
    public Double getCargoReparacion() {
        return getConfigByKey(CARGO_REPARACION).getConfigValue();
    }

    /**
     * RF4.1 y RF4.2: Actualizar configuración (solo Admin)
     */
    public ConfigEntity updateConfig(String key, Double newValue, String username) {
        if (newValue == null || newValue < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El valor debe ser mayor o igual a 0"
            );
        }

        ConfigEntity config = getConfigByKey(key);
        config.setConfigValue(newValue);
        config.setModifiedBy(username);
        config.setLastModified(LocalDateTime.now());

        return configRepository.save(config);
    }

    /**
     * RF4.1: Crear o actualizar tarifa de arriendo (solo Admin)
     */
    public ConfigEntity setTarifaArriendoDiaria(Double valor, String username) {
        return updateConfig(TARIFA_ARRIENDO_DIARIA, valor, username);
    }

    /**
     * RF4.2: Crear o actualizar tarifa de multa (solo Admin)
     */
    public ConfigEntity setTarifaMultaDiaria(Double valor, String username) {
        return updateConfig(TARIFA_MULTA_DIARIA, valor, username);
    }

    /**
     * ✅ NUEVO: Crear o actualizar cargo por reparación (solo Admin)
     * Épica 2 - RN #16: Configurar cargo por daños leves
     */
    public ConfigEntity setCargoReparacion(Double valor, String username) {
        return updateConfig(CARGO_REPARACION, valor, username);
    }

    /**
     * Obtener configuración por ID
     */
    public ConfigEntity getConfigById(Long id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Configuración no encontrada con ID: " + id
                ));
    }

    /**
     * Actualizar configuración por ID (solo Admin)
     */
    public ConfigEntity updateConfigById(Long id, Double newValue, String username) {
        ConfigEntity config = getConfigById(id);

        if (newValue == null || newValue < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El valor debe ser mayor o igual a 0"
            );
        }

        config.setConfigValue(newValue);
        config.setModifiedBy(username);
        config.setLastModified(LocalDateTime.now());

        return configRepository.save(config);
    }
}