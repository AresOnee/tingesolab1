package com.example.demo.Repository;

import com.example.demo.Entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolRepository extends JpaRepository<ToolEntity, Long> {
    boolean existsByNameIgnoreCase(String name);

    
}