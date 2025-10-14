package com.example.demo.Repository;

import com.example.demo.Controller.ReportController;
import com.example.demo.Entity.ToolEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    boolean existsByNameIgnoreCase(String name);

    // ========== NUEVAS QUERIES PARA ÉPICA 6 (REPORTES) ==========
    @Query("SELECT new com.example.demo.Controller.ReportController$ToolRanking(" +
            "t.id, t.name, COUNT(l.id)) " +
            "FROM ToolEntity t " +
            "LEFT JOIN LoanEntity l ON l.tool.id = t.id " +
            "GROUP BY t.id, t.name " +
            "ORDER BY COUNT(l.id) DESC")
    List<ReportController.ToolRanking> findMostLoanedTools(Pageable pageable);

    @Query("SELECT new com.example.demo.Controller.ReportController$ToolRanking(" +
            "t.id, t.name, COUNT(l.id)) " +
            "FROM ToolEntity t " +
            "LEFT JOIN LoanEntity l ON l.tool.id = t.id " +
            "AND l.startDate >= :startDate " +
            "AND l.startDate <= :endDate " +
            "GROUP BY t.id, t.name " +
            "ORDER BY COUNT(l.id) DESC")
    List<ReportController.ToolRanking> findMostLoanedToolsByDateRange(
                                                                        @Param("startDate") LocalDate startDate,
                                                                        @Param("endDate") LocalDate endDate,
                                                                        Pageable pageable
    );

    
}