package com.example.demo.Repository;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ToolRepositoryTest {

    @Autowired
    private ToolRepository toolRepository;

    @Test
    @DisplayName("Guardar y leer herramienta")
    void saveAndFind() {
        ToolEntity t = new ToolEntity();
        t.setName("Rotomartillo");
        t.setCategory("Eléctricas");
        t.setReplacementValue(120_000);
        t.setStock(3);
        t.setStatus("Disponible");

        ToolEntity saved = toolRepository.save(t);

        assertThat(saved.getId()).isNotNull();
        assertThat(toolRepository.findById(saved.getId())).isPresent();

        assertThat(toolRepository.existsByNameIgnoreCase("rotomartillo")).isTrue();
    }
}
