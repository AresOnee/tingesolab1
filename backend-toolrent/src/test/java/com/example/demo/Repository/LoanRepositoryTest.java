package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ToolRepository toolRepository;

    @Test
    @DisplayName("Guardar y leer préstamo")
    void saveAndFind() {
        ClientEntity c = new ClientEntity(null, "Juan", "12.345.678-9", "+569...", "juan@toolrent.cl", "Activo");
        c = clientRepository.save(c);

        ToolEntity t = new ToolEntity();
        t.setName("Taladro");
        t.setCategory("Eléctricas");
        t.setReplacementValue(80_000);
        t.setStock(2);
        t.setStatus("Disponible");
        t = toolRepository.save(t);

        LoanEntity l = new LoanEntity();
        l.setClient(c);
        l.setTool(t);
        l.setStartDate(LocalDate.now());
        l.setDueDate(LocalDate.now().plusDays(7));
        l.setStatus("Vigente");
        l.setFine(0d);
        l.setDamaged(false);
        l.setIrreparable(false);

        LoanEntity saved = loanRepository.save(l);

        assertThat(saved.getId()).isNotNull();
        assertThat(loanRepository.findById(saved.getId())).isPresent();
    }
}
