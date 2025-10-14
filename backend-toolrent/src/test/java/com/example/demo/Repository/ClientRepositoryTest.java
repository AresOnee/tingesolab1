package com.example.demo.Repository;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ClientEntity client1;
    private ClientEntity client2;
    private ClientEntity client3;
    private ToolEntity tool1;
    private ToolEntity tool2;

    @BeforeEach
    void setUp() {
        // Crear clientes de prueba
        client1 = new ClientEntity();
        client1.setName("Juan Perez");
        client1.setRut("12.345.678-9");
        client1.setEmail("juan@test.cl");
        client1.setPhone("+56911111111");
        client1.setState("Activo");
        client1 = entityManager.persist(client1);

        client2 = new ClientEntity();
        client2.setName("Maria Lopez");
        client2.setRut("98.765.432-1");
        client2.setEmail("maria@test.cl");
        client2.setPhone("+56922222222");
        client2.setState("Activo");
        client2 = entityManager.persist(client2);

        client3 = new ClientEntity();
        client3.setName("Pedro Gonzalez");
        client3.setRut("11.222.333-4");
        client3.setEmail("pedro@test.cl");
        client3.setPhone("+56933333333");
        client3.setState("Activo");
        client3 = entityManager.persist(client3);

        // Crear herramientas de prueba
        tool1 = new ToolEntity();
        tool1.setName("Taladro");
        tool1.setCategory("Eléctricas");
        tool1.setReplacementValue(80000);
        tool1.setStock(5);
        tool1.setStatus("Disponible");
        tool1 = entityManager.persist(tool1);

        tool2 = new ToolEntity();
        tool2.setName("Martillo");
        tool2.setCategory("Manuales");
        tool2.setReplacementValue(15000);
        tool2.setStock(10);
        tool2.setStatus("Disponible");
        tool2 = entityManager.persist(tool2);

        entityManager.flush();
    }

    // ========== TEST ORIGINAL ==========

    @Test
    @DisplayName("Guardar y leer un cliente")
    void saveAndFind() {
        ClientEntity c = new ClientEntity();
        c.setName("Test Cliente");
        c.setRut("99.888.777-6");
        c.setEmail("test@toolrent.cl");
        c.setPhone("+56999999999");
        c.setState("Activo");

        ClientEntity saved = clientRepository.save(c);

        assertThat(saved.getId()).isNotNull();
        assertThat(clientRepository.findById(saved.getId())).isPresent();
        assertThat(clientRepository.existsByRut("99.888.777-6")).isTrue();
        assertThat(clientRepository.existsByEmail("test@toolrent.cl")).isTrue();
    }

    // ========== NUEVOS TESTS ÉPICA 6 (RF6.2) ==========

    @Nested
    @DisplayName("Tests de findClientsWithOverdues() - RF6.2")
    class FindClientsWithOverduesTests {

        @Test
        @DisplayName("RF6.2: Debe retornar clientes con préstamos vencidos sin devolver")
        void findClientsWithOverdues_returnsClientsWithExpiredLoans() {
            // Given: Cliente con préstamo vencido
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(2),
                    null); // Sin devolver y vencido hace 2 días

            // Cliente sin atrasos (préstamo vigente)
            createLoan(client2, tool2,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(5),
                    null); // Sin devolver pero aún vigente

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(client1.getId());
            assertThat(result.get(0).getName()).isEqualTo("Juan Perez");
        }

        @Test
        @DisplayName("RF6.2: Debe excluir clientes con préstamos devueltos")
        void findClientsWithOverdues_excludesClientsWithReturnedLoans() {
            // Given: Cliente con préstamo vencido pero ya devuelto
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(2)); // Ya devuelto

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.2: Debe excluir clientes con préstamos vigentes (no vencidos)")
        void findClientsWithOverdues_excludesClientsWithActiveLoans() {
            // Given: Cliente con préstamo activo pero no vencido
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(5),
                    null); // Activo y vigente

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.2: Debe retornar DISTINCT (sin duplicar clientes con múltiples atrasos)")
        void findClientsWithOverdues_returnsDistinctClients() {
            // Given: Cliente con 3 préstamos vencidos
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(20),
                    LocalDate.now().minusDays(15),
                    null);

            createLoan(client1, tool2,
                    LocalDate.now().minusDays(15),
                    LocalDate.now().minusDays(10),
                    null);

            createLoan(client1, tool1,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    null);

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).hasSize(1); // Solo 1 cliente, no 3
            assertThat(result.get(0).getId()).isEqualTo(client1.getId());
        }

        @Test
        @DisplayName("RF6.2: Debe ordenar alfabéticamente por nombre")
        void findClientsWithOverdues_ordersAlphabetically() {
            // Given: 3 clientes con atrasos
            createLoan(client2, tool1, // Maria Lopez
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    null);

            createLoan(client1, tool2, // Juan Perez
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(3),
                    null);

            createLoan(client3, tool1, // Pedro Gonzalez
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(1),
                    null);

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).hasSize(3);
            // Orden alfabético: Juan, Maria, Pedro
            assertThat(result.get(0).getName()).isEqualTo("Juan Perez");
            assertThat(result.get(1).getName()).isEqualTo("Maria Lopez");
            assertThat(result.get(2).getName()).isEqualTo("Pedro Gonzalez");
        }

        @Test
        @DisplayName("RF6.2: Debe retornar lista vacía si no hay clientes con atrasos")
        void findClientsWithOverdues_returnsEmptyListWhenNoOverdues() {
            // Given: Solo préstamos vigentes o devueltos
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(5),
                    null); // Vigente

            createLoan(client2, tool2,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(3)); // Devuelto

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.2: Debe retornar lista vacía si no hay préstamos")
        void findClientsWithOverdues_returnsEmptyListWhenNoLoans() {
            // When (sin crear préstamos)
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.2: Debe considerar la fecha actual correctamente")
        void findClientsWithOverdues_usesCurrentDateCorrectly() {
            // Given: Préstamo que vence HOY (no debería estar atrasado aún)
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(5),
                    LocalDate.now(), // Vence HOY
                    null);

            // Préstamo vencido AYER (sí debería estar atrasado)
            createLoan(client2, tool2,
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(1), // Vence AYER
                    null);

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(client2.getId());
            assertThat(result).doesNotContain(client1);
        }

        @Test
        @DisplayName("RF6.2: Debe incluir clientes con estado 'Restringido' que tengan atrasos")
        void findClientsWithOverdues_includesRestrictedClients() {
            // Given: Cliente restringido con atraso
            client1.setState("Restringido");
            entityManager.persist(client1);

            createLoan(client1, tool1,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    null);

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(client1.getId());
            assertThat(result.get(0).getState()).isEqualTo("Restringido");
        }

        @Test
        @DisplayName("RF6.2: Debe manejar múltiples clientes con diferentes combinaciones de préstamos")
        void findClientsWithOverdues_handlesMixedScenarios() {
            // Given: Escenario complejo
            // Client1: 1 vencido, 1 vigente
            createLoan(client1, tool1,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(2),
                    null);
            createLoan(client1, tool2,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(5),
                    null);

            // Client2: Solo préstamos vigentes
            createLoan(client2, tool1,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(3),
                    null);

            // Client3: Préstamo vencido devuelto
            createLoan(client3, tool2,
                    LocalDate.now().minusDays(10),
                    LocalDate.now().minusDays(5),
                    LocalDate.now().minusDays(3));

            entityManager.flush();

            // When
            List<ClientEntity> result = clientRepository.findClientsWithOverdues();

            // Then: Solo client1 tiene atrasos SIN devolver
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(client1.getId());
        }
    }

    // ========== MÉTODOS HELPER ==========

    private LoanEntity createLoan(ClientEntity client, ToolEntity tool,
                                  LocalDate startDate, LocalDate dueDate,
                                  LocalDate returnDate) {
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(startDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(returnDate);
        loan.setStatus(returnDate == null ? "Vigente" : "Devuelto");
        loan.setFine(0.0);
        loan.setRentalCost(5000.0);
        loan.setDamaged(false);
        loan.setIrreparable(false);
        return entityManager.persist(loan);
    }
}