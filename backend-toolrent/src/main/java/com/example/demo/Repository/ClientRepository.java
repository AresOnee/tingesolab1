package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Actualizar solo el estado sin validaciones
     *
     * Este método actualiza directamente el campo 'state' en la base de datos
     * sin pasar por las validaciones de Bean Validation, lo cual es necesario
     * para la tarea programada que actualiza estados masivamente.
     *
     * @param clientId ID del cliente
     * @param newState Nuevo estado ("Activo" o "Restringido")
     */
    @Modifying
    @Query("UPDATE ClientEntity c SET c.state = :newState WHERE c.id = :clientId")
    void updateClientState(@Param("clientId") Long clientId, @Param("newState") String newState);

}

