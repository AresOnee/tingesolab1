package com.example.demo.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Entidad Cliente con validaciones completas (Gap Analysis - Punto 9)
 *
 * ✅ VALIDACIONES IMPLEMENTADAS:
 * - Campos obligatorios (@NotBlank)
 * - Formato de RUT chileno (@Pattern)
 * - Formato de email (@Email)
 * - Formato de teléfono chileno (@Pattern)
 * - Estados válidos (Activo/Restringido)
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "clients")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del cliente
     * Validaciones: Obligatorio, mínimo 2 caracteres
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres (Campo obligatorio)")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * RUT del cliente en formato chileno: 12.345.678-9
     * Validaciones:
     * - Obligatorio
     * - Formato: XX.XXX.XXX-X o X.XXX.XXX-X (con puntos y guión)
     * - Dígito verificador válido (0-9 o K)
     *
     * Ejemplos válidos:
     * - 12.345.678-9
     * - 1.234.567-K
     * - 20.204.010-5
     */
    @NotBlank(message = "El RUT es obligatorio")
    @Pattern(
            regexp = "^(?:\\s*|\\d{1,2}\\.\\d{3}\\.\\d{3}-[\\dkK])$",
            message = "Formato de RUT inválido. Use el formato: 12.345.678-9"
    )
    @Column(nullable = false, unique = true, length = 15)
    private String rut;

    /**
     * Teléfono del cliente en formato internacional chileno
     * Formato esperado: +56912345678
     * Validaciones:
     * - Obligatorio
     * - Debe iniciar con +56
     * - Seguido de 9 dígitos
     */
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(
            regexp = "^(?:\\s*|\\+56\\d{9})$",
            message = "Formato de teléfono inválido. Use el formato: +56912345678"
    )
    @Column(nullable = false, length = 20)
    private String phone;

    /**
     * Email del cliente
     * Validaciones:
     * - Obligatorio
     * - Formato válido de email
     * - Único en la base de datos
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Estado del cliente
     * Valores permitidos:
     * - "Activo": Puede solicitar préstamos
     * - "Restringido": No puede solicitar préstamos hasta regularizar deudas
     *
     * Por defecto se asigna "Activo" al crear un cliente
     */
    @NotBlank(message = "El estado es obligatorio")
    @Pattern(
            regexp = "^(?:\\s*|(Activo|Restringido))$",
            message = "Estado inválido. Valores permitidos: Activo, Restringido"
    )
    @Column(nullable = false, length = 20)
    private String state;

    // Constructor sin state (se asigna "Activo" por defecto)
    public ClientEntity(Long id, String name, String rut, String phone, String email) {
        this.id = id;
        this.name = name;
        this.rut = rut;
        this.phone = phone;
        this.email = email;
        this.state = "Activo";
    }
}