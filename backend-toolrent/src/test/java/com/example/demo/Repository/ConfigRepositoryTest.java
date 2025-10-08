package com.example.demo.Repository;

import com.example.demo.Entity.ConfigEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integración para ConfigRepository
 * Verifica operaciones de persistencia y consultas personalizadas
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
class ConfigRepositoryTest {

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Test básico: guardar y leer una configuración
     */
    @Test
    @DisplayName("Guardar y leer una configuración")
    void saveAndFind() {
        // Given: Una configuración nueva
        ConfigEntity config = new ConfigEntity();
        config.setConfigKey("TARIFA_TEST");
        config.setConfigValue(1000.0);
        config.setDescription("Tarifa de prueba");
        config.setLastModified(LocalDateTime.now());
        config.setModifiedBy("test-user");

        // When: Se guarda
        ConfigEntity saved = configRepository.save(config);
        entityManager.flush();

        // Then: Se puede recuperar por ID
        assertThat(saved.getId()).isNotNull();
        Optional<ConfigEntity> found = configRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getConfigKey()).isEqualTo("TARIFA_TEST");
        assertThat(found.get().getConfigValue()).isEqualTo(1000.0);
    }

    /**
     * Test: findByConfigKey - encontrar configuración existente
     */
    @Test
    @DisplayName("findByConfigKey: debe encontrar configuración existente")
    void findByConfigKey_found() {
        // Given: Una configuración guardada
        ConfigEntity config = createConfig("TARIFA_ARRIENDO_DIARIA", 5000.0);
        entityManager.persist(config);
        entityManager.flush();

        // When: Se busca por clave
        Optional<ConfigEntity> result = configRepository.findByConfigKey("TARIFA_ARRIENDO_DIARIA");

        // Then: Se encuentra la configuración
        assertThat(result).isPresent();
        assertThat(result.get().getConfigKey()).isEqualTo("TARIFA_ARRIENDO_DIARIA");
        assertThat(result.get().getConfigValue()).isEqualTo(5000.0);
    }

    /**
     * Test: findByConfigKey - clave inexistente
     */
    @Test
    @DisplayName("findByConfigKey: debe retornar empty cuando no existe")
    void findByConfigKey_notFound() {
        // When: Se busca una clave que no existe
        Optional<ConfigEntity> result = configRepository.findByConfigKey("CLAVE_INEXISTENTE");

        // Then: Retorna empty
        assertThat(result).isEmpty();
    }

    /**
     * Test: existsByConfigKey - clave existente
     */
    @Test
    @DisplayName("existsByConfigKey: debe retornar true si existe")
    void existsByConfigKey_exists() {
        // Given: Una configuración guardada
        ConfigEntity config = createConfig("TARIFA_MULTA_DIARIA", 2000.0);
        entityManager.persist(config);
        entityManager.flush();

        // When: Se verifica existencia
        boolean exists = configRepository.existsByConfigKey("TARIFA_MULTA_DIARIA");

        // Then: Retorna true
        assertThat(exists).isTrue();
    }

    /**
     * Test: existsByConfigKey - clave inexistente
     */
    @Test
    @DisplayName("existsByConfigKey: debe retornar false si no existe")
    void existsByConfigKey_notExists() {
        // When: Se verifica clave inexistente
        boolean exists = configRepository.existsByConfigKey("CLAVE_INEXISTENTE");

        // Then: Retorna false
        assertThat(exists).isFalse();
    }

    /**
     * Test: Constraint UNIQUE en configKey
     */
    @Test
    @DisplayName("No debe permitir claves duplicadas (UNIQUE constraint)")
    void uniqueConfigKey_violation() {
        // Given: Una configuración guardada
        ConfigEntity config1 = createConfig("TARIFA_TEST", 1000.0);
        entityManager.persist(config1);
        entityManager.flush();

        // When: Se intenta guardar otra con la misma clave
        ConfigEntity config2 = createConfig("TARIFA_TEST", 2000.0);

        // Then: Debe lanzar excepción
        assertThatThrownBy(() -> {
            configRepository.save(config2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Test: Actualizar valor de configuración existente
     */
    @Test
    @DisplayName("Actualizar valor de configuración existente")
    void updateConfigValue() {
        // Given: Una configuración guardada
        ConfigEntity config = createConfig("TARIFA_ARRIENDO_DIARIA", 5000.0);
        config = entityManager.persist(config);
        entityManager.flush();
        Long configId = config.getId();

        // When: Se actualiza el valor
        config.setConfigValue(6000.0);
        config.setModifiedBy("admin");
        config.setLastModified(LocalDateTime.now());
        configRepository.save(config);
        entityManager.flush();
        entityManager.clear(); // Limpiar cache

        // Then: El valor se actualizó
        ConfigEntity updated = configRepository.findById(configId).orElseThrow();
        assertThat(updated.getConfigValue()).isEqualTo(6000.0);
        assertThat(updated.getModifiedBy()).isEqualTo("admin");
    }

    /**
     * Test: Eliminar configuración
     */
    @Test
    @DisplayName("Eliminar configuración")
    void deleteConfig() {
        // Given: Una configuración guardada
        ConfigEntity config = createConfig("TARIFA_TEST", 1000.0);
        config = entityManager.persist(config);
        entityManager.flush();
        Long configId = config.getId();

        // When: Se elimina
        configRepository.deleteById(configId);
        entityManager.flush();

        // Then: Ya no existe
        assertThat(configRepository.findById(configId)).isEmpty();
    }

    /**
     * Test: Listar todas las configuraciones
     */
    @Test
    @DisplayName("Listar todas las configuraciones")
    void findAll() {
        // Given: Múltiples configuraciones
        entityManager.persist(createConfig("TARIFA_ARRIENDO_DIARIA", 5000.0));
        entityManager.persist(createConfig("TARIFA_MULTA_DIARIA", 2000.0));
        entityManager.persist(createConfig("TARIFA_REPARACION", 3000.0));
        entityManager.flush();

        // When: Se listan todas
        var configs = configRepository.findAll();

        // Then: Se obtienen todas
        assertThat(configs).hasSize(3);
        assertThat(configs)
                .extracting(ConfigEntity::getConfigKey)
                .containsExactlyInAnyOrder(
                        "TARIFA_ARRIENDO_DIARIA",
                        "TARIFA_MULTA_DIARIA",
                        "TARIFA_REPARACION"
                );
    }

    /**
     * Test: Validación de campo NOT NULL (configKey)
     */
    @Test
    @DisplayName("configKey no puede ser null")
    void configKey_notNull() {
        // Given: Configuración sin configKey
        ConfigEntity config = new ConfigEntity();
        config.setConfigKey(null); // null
        config.setConfigValue(1000.0);
        config.setLastModified(LocalDateTime.now());

        // When/Then: Debe fallar al persistir
        assertThatThrownBy(() -> {
            configRepository.save(config);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // Puede ser ConstraintViolationException o DataIntegrityViolationException
    }

    /**
     * Test: Validación de campo NOT NULL (configValue)
     */
    @Test
    @DisplayName("configValue no puede ser null")
    void configValue_notNull() {
        // Given: Configuración sin configValue
        ConfigEntity config = new ConfigEntity();
        config.setConfigKey("TARIFA_TEST");
        config.setConfigValue(null); // null
        config.setLastModified(LocalDateTime.now());

        // When/Then: Debe fallar al persistir
        assertThatThrownBy(() -> {
            configRepository.save(config);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }

    /**
     * Test: Búsqueda case-sensitive en configKey
     */
    @Test
    @DisplayName("Búsqueda de configKey es case-sensitive")
    void findByConfigKey_caseSensitive() {
        // Given: Configuración con clave en mayúsculas
        entityManager.persist(createConfig("TARIFA_ARRIENDO_DIARIA", 5000.0));
        entityManager.flush();

        // When: Se busca con minúsculas
        Optional<ConfigEntity> result = configRepository.findByConfigKey("tarifa_arriendo_diaria");

        // Then: No se encuentra (case-sensitive)
        assertThat(result).isEmpty();
    }

    /**
     * Test: @PrePersist actualiza lastModified automáticamente
     */
    @Test
    @DisplayName("@PrePersist debe setear lastModified automáticamente")
    void prePersist_setsLastModified() {
        // Given: Configuración sin lastModified
        ConfigEntity config = new ConfigEntity();
        config.setConfigKey("TARIFA_TEST");
        config.setConfigValue(1000.0);
        config.setModifiedBy("test");
        // No seteamos lastModified

        // When: Se persiste
        ConfigEntity saved = configRepository.save(config);
        entityManager.flush();

        // Then: lastModified se setea automáticamente
        assertThat(saved.getLastModified()).isNotNull();
        assertThat(saved.getLastModified()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Crea una configuración de prueba
     */
    private ConfigEntity createConfig(String key, Double value) {
        ConfigEntity config = new ConfigEntity();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription("Descripción de " + key);
        config.setLastModified(LocalDateTime.now());
        config.setModifiedBy("test-user");
        return config;
    }
}