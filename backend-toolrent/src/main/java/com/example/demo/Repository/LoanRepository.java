package com.example.demo.Repository;

import com.example.demo.Entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    // Cuenta los préstamos activos ("Vigente" o "Atrasado") de un cliente
    @Query("SELECT COUNT(l) FROM LoanEntity l WHERE l.client.id = :clientId AND (l.status = 'Vigente' OR l.status = 'Atrasado')")
    long countActiveByClientId(@Param("clientId") Long clientId);

    // Verifica si un cliente tiene un préstamo activo con una herramienta específica
    @Query("SELECT (COUNT(l) > 0) FROM LoanEntity l WHERE l.client.id = :clientId AND l.tool.id = :toolId AND (l.status = 'Vigente' OR l.status = 'Atrasado')")
    boolean existsActiveByClientAndTool(@Param("clientId") Long clientId, @Param("toolId") Long toolId);

    // Verifica si un cliente tiene préstamos vencidos o multas (deudas) impagas
    @Query("SELECT (COUNT(l) > 0) FROM LoanEntity l WHERE l.client.id = :clientId AND (l.status = 'Atrasado' OR l.fine > 0)")
    boolean hasOverduesOrFines(@Param("clientId") Long clientId);
}
