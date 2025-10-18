package com.example.demo.Controller;

import com.example.demo.Utils.KeycloakUtils;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Service.ToolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    /**
     * ✅ CORREGIDO: Ahora usa KeycloakUtils para obtener el username real
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping({ "", "/" })
    public ResponseEntity<ToolEntity> create(
            @Valid @RequestBody ToolEntity body,
            Authentication authentication  // ← Agregado
    ) {
        // ✅ Obtener username real de Keycloak en lugar de usar "ADMIN"
        String username = KeycloakUtils.getUsername(authentication);

        ToolEntity saved = toolService.create(body, username);  // ← Pasar username al service

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    /**
     * RF1.2: Dar de baja herramientas dañadas o en desuso (solo Administrador)
     * PUT /api/v1/tools/{id}/decommission
     * ✅ CORREGIDO: Ahora usa KeycloakUtils para obtener el username real
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/decommission")
    public ResponseEntity<ToolEntity> decommissionTool(
            @PathVariable Long id,
            Authentication authentication  // ← Agregado
    ) {
        // ✅ Obtener username real de Keycloak en lugar de usar "ADMIN"
        String username = KeycloakUtils.getUsername(authentication);

        ToolEntity decommissioned = toolService.decommission(id, username);  // ← Pasar username al service
        return ResponseEntity.ok(decommissioned);
    }
}