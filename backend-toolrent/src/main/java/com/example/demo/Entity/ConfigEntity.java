package com.example.demo.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para gestionar configuraciones y tarifas del sistema
 * RF4.1, RF4.2, RF4.3: Tarifas configurables
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_config")
public class ConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Clave única de configuración
     * Ej: "TARIFA_ARRIENDO_DIARIA", "TARIFA_MULTA_DIARIA"
     */
    @NotBlank(message = "La clave es obligatoria")
    @Column(unique = true, nullable = false, length = 100)
    private String configKey;

    /**
     * Valor de la configuración
     */
    @NotNull(message = "El valor es obligatorio")
    @Min(value = 0, message = "El valor no puede ser negativo")
    private Double configValue;

    /**
     * Descripción de la configuración
     */
    @Column(length = 255)
    private String description;

    /**
     * Última fecha de modificación
     */
    @Column(nullable = false)
    private LocalDateTime lastModified;

    /**
     * Usuario que realizó la última modificación
     */
    private String modifiedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastModified = LocalDateTime.now();
    }
}