package com.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loans")
public class LoanEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_loans_client"))
    private ClientEntity client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tool_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_loans_tool"))
    private ToolEntity tool;

    private LocalDate startDate;   // se setea al crear
    private LocalDate dueDate;     // fecha de vencimiento
    private LocalDate returnDate;  // al devolver

    private String status;         // "ACTIVO", "ATRASADO", "CERRADO"
    private Double fine;           // multa por atraso

    // ✅ NUEVO: Costo del arriendo (para mostrar en UI)
    private Double rentalCost;     // costo del arriendo = días × tarifa

    private Boolean damaged;       // devuelta dañada?
    private Boolean irreparable;   // daño irreparable?

    // getters/setters
}