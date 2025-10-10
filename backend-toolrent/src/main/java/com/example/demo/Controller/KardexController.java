package com.example.demo.Controller;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Service.KardexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kardex")
@Tag(name = "Kardex", description = "Gestión de movimientos de inventario (Épica 5)")
@SecurityRequirement(name = "bearerAuth")
public class KardexController {

    @Autowired
    private KardexService kardexService;

    /**
     * RF5.2: Consultar historial de movimientos por herramienta
     */
    @GetMapping("/tool/{toolId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Obtener movimientos de una herramienta específica")
    public ResponseEntity<List<KardexEntity>> getMovementsByTool(@PathVariable Long toolId) {
        List<KardexEntity> movements = kardexService.getMovementsByTool(toolId);
        return ResponseEntity.ok(movements);
    }

    /**
     * RF5.3: Generar listado de movimientos por rango de fechas
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Obtener movimientos por rango de fechas")
    public ResponseEntity<List<KardexEntity>> getMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<KardexEntity> movements = kardexService.getMovementsByDateRange(startDate, endDate);
        return ResponseEntity.ok(movements);
    }

    /**
     * Obtener todos los movimientos
     */
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Obtener todos los movimientos del kardex")
    public ResponseEntity<List<KardexEntity>> getAllMovements() {
        List<KardexEntity> movements = kardexService.getAllMovements();
        return ResponseEntity.ok(movements);
    }

    /**
     * Obtener movimientos por tipo
     */
    @GetMapping("/type/{movementType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Obtener movimientos por tipo (REGISTRO, PRESTAMO, DEVOLUCION, BAJA, REPARACION)")
    public ResponseEntity<List<KardexEntity>> getMovementsByType(@PathVariable String movementType) {
        List<KardexEntity> movements = kardexService.getMovementsByType(movementType);
        return ResponseEntity.ok(movements);
    }
}