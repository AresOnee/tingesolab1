package com.example.demo.Controller;

import com.example.demo.Entity.LoanEntity;
import com.example.demo.Service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")

public class LoanController {

    @Autowired
    private LoanService loanService;

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/create")
    public LoanEntity createLoan(@RequestParam Long clientId,
                                 @RequestParam Long toolId,
                                 @RequestParam String dueDate) {
        return loanService.createLoan(clientId, toolId, LocalDate.parse(dueDate));
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping("/return")
    public LoanEntity returnLoan(@RequestParam Long loanId,
                                 @RequestParam boolean isDamaged,
                                 @RequestParam boolean isIrreparable) {
        return loanService.returnTool(loanId, isDamaged, isIrreparable);
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/")
    public List<LoanEntity> getAllLoans() {
        return loanService.getAllLoans();
    }
}