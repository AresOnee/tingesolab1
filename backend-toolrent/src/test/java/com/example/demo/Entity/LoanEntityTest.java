package com.example.demo.Entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para LoanEntity
 * Incluye tests para el nuevo campo rentalCost (Mejora 2)
 */
@DisplayName("Tests de LoanEntity")
class LoanEntityTest {

    @Test
    @DisplayName("Constructor sin argumentos debe crear entidad vacía")
    void noArgsConstructor() {
        // When
        LoanEntity loan = new LoanEntity();

        // Then
        assertThat(loan).isNotNull();
        assertThat(loan.getId()).isNull();
        assertThat(loan.getClient()).isNull();
        assertThat(loan.getTool()).isNull();
        assertThat(loan.getStartDate()).isNull();
        assertThat(loan.getDueDate()).isNull();
        assertThat(loan.getReturnDate()).isNull();
        assertThat(loan.getStatus()).isNull();
        assertThat(loan.getFine()).isNull();
        assertThat(loan.getRentalCost()).isNull();
        assertThat(loan.getDamaged()).isNull();
        assertThat(loan.getIrreparable()).isNull();
    }

    @Test
    @DisplayName("Debe permitir setear y obtener rentalCost")
    void setAndGetRentalCost() {
        // Given
        LoanEntity loan = new LoanEntity();
        Double expectedCost = 35000.0;

        // When
        loan.setRentalCost(expectedCost);

        // Then
        assertThat(loan.getRentalCost()).isEqualTo(expectedCost);
    }

    @Test
    @DisplayName("Constructor con todos los argumentos debe incluir rentalCost")
    void allArgsConstructor() {
        // Given
        ClientEntity client = new ClientEntity(1L, "Juan", "12.345.678-9",
                "+569...", "juan@test.cl", "Activo");
        ToolEntity tool = new ToolEntity(1L, "Taladro", "Eléctricas",
                "Disponible", 50000, 5);
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(7);
        Double rentalCost = 35000.0;

        // When
        // ✅ ORDEN CORRECTO: id, client, tool, startDate, dueDate, returnDate,
        //                     status, fine, rentalCost, damaged, irreparable
        LoanEntity loan = new LoanEntity(
                1L, client, tool, startDate, dueDate, null,
                "Vigente", 0.0, rentalCost, false, false  // ✅ rentalCost ANTES de damaged/irreparable
        );

        // Then
        assertThat(loan.getRentalCost()).isEqualTo(rentalCost);
        assertThat(loan.getClient()).isEqualTo(client);
        assertThat(loan.getTool()).isEqualTo(tool);
        assertThat(loan.getStartDate()).isEqualTo(startDate);
        assertThat(loan.getDueDate()).isEqualTo(dueDate);
        assertThat(loan.getStatus()).isEqualTo("Vigente");
        assertThat(loan.getFine()).isEqualTo(0.0);
        assertThat(loan.getDamaged()).isFalse();
        assertThat(loan.getIrreparable()).isFalse();
    }

    @Test
    @DisplayName("rentalCost puede ser null")
    void rentalCostCanBeNull() {
        // Given
        LoanEntity loan = new LoanEntity();

        // When
        loan.setRentalCost(null);

        // Then
        assertThat(loan.getRentalCost()).isNull();
    }

    @Test
    @DisplayName("rentalCost puede ser cero")
    void rentalCostCanBeZero() {
        // Given
        LoanEntity loan = new LoanEntity();

        // When
        loan.setRentalCost(0.0);

        // Then
        assertThat(loan.getRentalCost()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("rentalCost puede ser negativo (edge case)")
    void rentalCostCanBeNegative() {
        // Given
        LoanEntity loan = new LoanEntity();

        // When
        loan.setRentalCost(-100.0);

        // Then
        assertThat(loan.getRentalCost()).isEqualTo(-100.0);
    }

    @Test
    @DisplayName("rentalCost puede manejar valores grandes")
    void rentalCostCanHandleLargeValues() {
        // Given
        LoanEntity loan = new LoanEntity();
        Double largeCost = 1000000.0; // 1 millón

        // When
        loan.setRentalCost(largeCost);

        // Then
        assertThat(loan.getRentalCost()).isEqualTo(largeCost);
    }

    @Test
    @DisplayName("Todos los setters y getters deben funcionar correctamente")
    void allSettersAndGettersWork() {
        // Given
        LoanEntity loan = new LoanEntity();
        ClientEntity client = new ClientEntity(1L, "Juan", "12.345.678-9",
                "+569...", "juan@test.cl", "Activo");
        ToolEntity tool = new ToolEntity(1L, "Taladro", "Eléctricas",
                "Disponible", 50000, 5);
        LocalDate startDate = LocalDate.now();
        LocalDate dueDate = LocalDate.now().plusDays(7);
        LocalDate returnDate = LocalDate.now().plusDays(5);

        // When
        loan.setId(100L);
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(startDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(returnDate);
        loan.setStatus("Vigente");
        loan.setFine(5000.0);
        loan.setRentalCost(35000.0);
        loan.setDamaged(false);
        loan.setIrreparable(false);

        // Then
        assertThat(loan.getId()).isEqualTo(100L);
        assertThat(loan.getClient()).isEqualTo(client);
        assertThat(loan.getTool()).isEqualTo(tool);
        assertThat(loan.getStartDate()).isEqualTo(startDate);
        assertThat(loan.getDueDate()).isEqualTo(dueDate);
        assertThat(loan.getReturnDate()).isEqualTo(returnDate);
        assertThat(loan.getStatus()).isEqualTo("Vigente");
        assertThat(loan.getFine()).isEqualTo(5000.0);
        assertThat(loan.getRentalCost()).isEqualTo(35000.0);
        assertThat(loan.getDamaged()).isFalse();
        assertThat(loan.getIrreparable()).isFalse();
    }
}