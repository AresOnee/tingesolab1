package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    @DisplayName("Guardar y leer un cliente")
    void saveAndFind() {
        ClientEntity c = new ClientEntity();
        c.setName("Juan Perez");
        c.setRut("12.345.678-9");
        c.setEmail("juan@toolrent.cl");
        c.setPhone("+56911111111");
        c.setState("Activo");

        ClientEntity saved = clientRepository.save(c);

        assertThat(saved.getId()).isNotNull();
        assertThat(clientRepository.findById(saved.getId())).isPresent();
        assertThat(clientRepository.existsByRut("12.345.678-9")).isTrue();
        assertThat(clientRepository.existsByEmail("juan@toolrent.cl")).isTrue();
    }
}
