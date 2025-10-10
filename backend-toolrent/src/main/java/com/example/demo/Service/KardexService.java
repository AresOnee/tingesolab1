package com.example.demo.Service;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.KardexRepository;
import com.example.demo.Repository.ToolRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class KardexService {

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private ToolRepository toolRepository;

    /**
     * RF5.1: Registrar movimiento en el kardex
     */
    @Transactional
    public KardexEntity registerMovement(
            Long toolId,
            String movementType,
            Integer quantity,
            String username,
            String observations,
            Long loanId
    ) {
        // Validaciones
        if (toolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de herramienta es obligatorio");
        }
        if (movementType == null || movementType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de movimiento es obligatorio");
        }
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad es obligatoria");
        }

        // Buscar herramienta
        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Herramienta no encontrada con ID: " + toolId
                ));

        // Crear movimiento
        KardexEntity kardex = new KardexEntity();
        kardex.setMovementType(movementType.toUpperCase());
        kardex.setTool(tool);
        kardex.setQuantity(quantity);
        kardex.setUsername(username != null ? username : "SYSTEM");
        kardex.setMovementDate(LocalDateTime.now());
        kardex.setObservations(observations);
        kardex.setLoanId(loanId);

        return kardexRepository.save(kardex);
    }

    /**
     * RF5.2: Consultar historial de movimientos por herramienta
     */
    public List<KardexEntity> getMovementsByTool(Long toolId) {
        if (toolId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de herramienta es obligatorio");
        }

        ToolEntity tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Herramienta no encontrada con ID: " + toolId
                ));

        return kardexRepository.findByToolOrderByMovementDateDesc(tool);
    }

    /**
     * RF5.3: Generar listado de movimientos por rango de fechas
     */
    public List<KardexEntity> getMovementsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fechas son obligatorias");
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser anterior a la fecha de fin");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        return kardexRepository.findByMovementDateBetweenOrderByMovementDateDesc(startDateTime, endDateTime);
    }

    /**
     * Obtener todos los movimientos
     */
    public List<KardexEntity> getAllMovements() {
        return kardexRepository.findAllByOrderByMovementDateDesc();
    }

    /**
     * Obtener movimientos por tipo
     */
    public List<KardexEntity> getMovementsByType(String movementType) {
        if (movementType == null || movementType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de movimiento es obligatorio");
        }
        return kardexRepository.findByMovementTypeOrderByMovementDateDesc(movementType.toUpperCase());
    }
}