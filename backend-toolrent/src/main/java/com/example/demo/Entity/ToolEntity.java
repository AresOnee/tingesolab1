package com.example.demo.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "tools",
        uniqueConstraints = @UniqueConstraint(name = "uq_tools_name", columnNames = "name")
)
public class ToolEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "La categoría es obligatoria")
    private String category;

    @NotBlank(message = "El estado es obligatorio")
    private String status;

    @Min(value = 1, message = "replacementValue debe ser mayor a 0")
    private int replacementValue;

    @Min(value = 0, message = "stock no puede ser negativo")
    private int stock;

}
