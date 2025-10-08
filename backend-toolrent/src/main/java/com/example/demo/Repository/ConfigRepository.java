package com.example.demo.Repository;

import com.example.demo.Entity.ConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

    /**
     * Buscar configuración por clave
     */
    Optional<ConfigEntity> findByConfigKey(String configKey);

    /**
     * Verificar si existe una clave
     */
    boolean existsByConfigKey(String configKey);
}