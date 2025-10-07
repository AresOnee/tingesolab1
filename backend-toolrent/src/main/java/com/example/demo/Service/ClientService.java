package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<ClientEntity> getAll() {
        return clientRepository.findAll();
    }

    @Transactional
    public ClientEntity create(ClientEntity body) {
        if (clientRepository.existsByRut(body.getRut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El RUT ya existe");
        }
        if (clientRepository.existsByEmail(body.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe");
        }

        if (body.getState() == null || body.getState().isBlank()) {
            body.setState("Activo");
        }

        return clientRepository.save(body);
    }



}
