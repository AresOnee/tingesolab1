package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

        boolean existsByRut(String rut);      // para el prechequeo
        boolean existsByEmail(String email);  // para el prechequeo
    // ========== QUERY PARA ÉPICA 6 (REPORTES) ==========

    /**
     * RF6.2: Obtener clientes que tienen préstamos atrasados
     *
     * Un cliente tiene atrasos si:
     * - Tiene préstamos sin devolver (returnDate = null)
     * - Y la fecha de vencimiento ya pasó (dueDate < fecha actual)
     *
     * DISTINCT para evitar duplicados si un cliente tiene múltiples préstamos atrasados
     * Ordenados alfabéticamente por nombre
     */
    @Query("SELECT DISTINCT c FROM ClientEntity c " +
            "JOIN LoanEntity l ON l.client.id = c.id " +
            "WHERE l.returnDate IS NULL " +
            "AND l.dueDate < CURRENT_DATE " +
            "ORDER BY c.name")
    List<ClientEntity> findClientsWithOverdues();

}

