package com.example.demo.Controller;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.ToolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tools")

public class ToolController {
    @Autowired
    private ToolService toolService;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping({ "", "/" })
    public List<ToolEntity> getAllTools() {
        return toolService.getAllTools();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({ "", "/" })
    public ResponseEntity<ToolEntity> create(@Valid @RequestBody ToolEntity body) {
        ToolEntity saved = toolService.create(body);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }
}