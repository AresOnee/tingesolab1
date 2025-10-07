package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

        boolean existsByRut(String rut);      // para el prechequeo
        boolean existsByEmail(String email);  // para el prechequeo

}

