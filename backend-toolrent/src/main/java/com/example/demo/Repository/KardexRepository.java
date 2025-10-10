package com.example.demo.Repository;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    /**
     * RF5.2: Consultar historial de movimientos por herramienta
     */
    List<KardexEntity> findByToolOrderByMovementDateDesc(ToolEntity tool);

    /**
     * RF5.3: Generar listado de movimientos por rango de fechas
     */
    List<KardexEntity> findByMovementDateBetweenOrderByMovementDateDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Obtener todos los movimientos ordenados por fecha descendente
     */
    List<KardexEntity> findAllByOrderByMovementDateDesc();

    /**
     * Buscar movimientos por tipo
     */
    List<KardexEntity> findByMovementTypeOrderByMovementDateDesc(String movementType);

    /**
     * Buscar movimientos por herramienta y tipo
     */
    List<KardexEntity> findByToolAndMovementTypeOrderByMovementDateDesc(
            ToolEntity tool,
            String movementType
    );
}