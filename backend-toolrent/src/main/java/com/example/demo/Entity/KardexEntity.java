package com.example.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kardex")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KardexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo de movimiento:
     * - REGISTRO (alta de herramienta)
     * - PRESTAMO
     * - DEVOLUCION
     * - BAJA
     * - REPARACION
     */
    @NotBlank(message = "El tipo de movimiento es obligatorio")
    @Column(nullable = false, length = 50)
    private String movementType;

    /**
     * Herramienta afectada por el movimiento
     */
    @NotNull(message = "La herramienta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ToolEntity tool;

    /**
     * Cantidad afectada en el movimiento
     * - Positivo: incrementa stock (registro, devolución)
     * - Negativo: reduce stock (préstamo, baja)
     */
    @NotNull(message = "La cantidad es obligatoria")
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Usuario que realizó el movimiento
     */
    @Column(length = 100)
    private String username;

    /**
     * Fecha y hora del movimiento
     */
    @NotNull(message = "La fecha de movimiento es obligatoria")
    @Column(nullable = false)
    private LocalDateTime movementDate;

    /**
     * Observaciones adicionales del movimiento
     */
    @Column(columnDefinition = "TEXT")
    private String observations;

    /**
     * Referencia opcional al préstamo asociado (si aplica)
     */
    @Column(name = "loan_id")
    private Long loanId;
}