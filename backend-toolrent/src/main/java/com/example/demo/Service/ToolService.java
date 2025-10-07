package com.example.demo.Service;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ToolRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ToolService {

    @Autowired private ToolRepository toolRepository;
    public List<ToolEntity> getAllTools() {
        return toolRepository.findAll();
    }

    public ToolEntity saveTool(ToolEntity tool) {
        if (tool.getStatus() == null || tool.getStatus().isBlank()) {
            tool.setStatus("Disponible");
        }
        return toolRepository.save(tool);
    }

    public ToolEntity create(ToolEntity body) {
        String name = Optional.ofNullable(body.getName()).orElse("").trim();
        if (name.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (toolRepository.existsByNameIgnoreCase(name)) {
            throw new DataIntegrityViolationException("uq_tools_name"); // dispara el 409 del handler
        }
        body.setName(name);
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("Disponible");
        }
        return toolRepository.save(body);
    }

}
