package com.example.demo.Controller;

import com.example.demo.Utils.KeycloakUtils;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    /**
     * RF2.1: Crear préstamo
     * ✅ CORREGIDO: Usa KeycloakUtils para obtener username real
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/create")
    public LoanEntity createLoan(
            @RequestParam Long clientId,
            @RequestParam Long toolId,
            @RequestParam String dueDate,
            Authentication authentication
    ) {
        // ✅ Obtener username real de Keycloak
        String username = KeycloakUtils.getUsername(authentication);

        // ✅ Pasar username al service (4 parámetros)
        return loanService.createLoan(clientId, toolId, LocalDate.parse(dueDate), username);
    }

    /**
     * RF2.3: Registrar devolución
     * ✅ CORREGIDO: Usa KeycloakUtils para obtener username real
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/return")
    public LoanEntity returnLoan(
            @RequestParam Long loanId,
            @RequestParam boolean isDamaged,
            @RequestParam boolean isIrreparable,
            Authentication authentication
    ) {
        // ✅ Obtener username real de Keycloak
        String username = KeycloakUtils.getUsername(authentication);

        // ✅ Pasar username al service (4 parámetros)
        return loanService.returnTool(loanId, isDamaged, isIrreparable, username);
    }

    /**
     * RF2.5: Listar todos los préstamos
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/")
    public List<LoanEntity> getAllLoans() {
        return loanService.getAllLoans();
    }
}