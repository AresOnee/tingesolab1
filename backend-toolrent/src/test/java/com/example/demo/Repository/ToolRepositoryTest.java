package com.example.demo.Repository;

import com.example.demo.Controller.ReportController;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
class ToolRepositoryTest {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity tool1;
    private ToolEntity tool2;
    private ToolEntity tool3;

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

        tool3 = new ToolEntity();
        tool3.setName("Sierra");
        tool3.setCategory("Corte");
        tool3.setReplacementValue(50000);
        tool3.setStock(3);
        tool3.setStatus("Disponible");
        tool3 = entityManager.persist(tool3);

        entityManager.flush();
    }

    // ========== TEST ORIGINAL ==========

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

    // ========== NUEVOS TESTS ÉPICA 6 (RF6.3) ==========

    @Nested
    @DisplayName("Tests de findMostLoanedTools() - RF6.3")
    class FindMostLoanedToolsTests {

        @Test
        @DisplayName("RF6.3: Debe retornar herramientas ordenadas por cantidad de préstamos (DESC)")
        void findMostLoanedTools_ordersByCountDesc() {
            // Given: Tool1 con 5 préstamos, Tool2 con 3, Tool3 con 1
            for (int i = 0; i < 5; i++) {
                createLoan(client1, tool1, LocalDate.now().minusDays(i));
            }
            for (int i = 0; i < 3; i++) {
                createLoan(client2, tool2, LocalDate.now().minusDays(i));
            }
            createLoan(client1, tool3, LocalDate.now());

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedTools(PageRequest.of(0, 10));

            // Then
            assertThat(result).hasSize(3);

            // Orden: Tool1 (5), Tool2 (3), Tool3 (1)
            assertThat(result.get(0).getToolId()).isEqualTo(tool1.getId());
            assertThat(result.get(0).getToolName()).isEqualTo("Taladro");
            assertThat(result.get(0).getLoanCount()).isEqualTo(5L);

            assertThat(result.get(1).getToolId()).isEqualTo(tool2.getId());
            assertThat(result.get(1).getToolName()).isEqualTo("Martillo");
            assertThat(result.get(1).getLoanCount()).isEqualTo(3L);

            assertThat(result.get(2).getToolId()).isEqualTo(tool3.getId());
            assertThat(result.get(2).getToolName()).isEqualTo("Sierra");
            assertThat(result.get(2).getLoanCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("RF6.3: Debe respetar el límite (Pageable)")
        void findMostLoanedTools_respectsLimit() {
            // Given: 3 herramientas con préstamos
            createLoan(client1, tool1, LocalDate.now());
            createLoan(client1, tool2, LocalDate.now());
            createLoan(client1, tool3, LocalDate.now());

            entityManager.flush();

            // When: Solicitar top 2
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedTools(PageRequest.of(0, 2));

            // Then: Solo 2 resultados
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("RF6.3: Debe incluir herramientas sin préstamos con count = 0")
        void findMostLoanedTools_includesToolsWithZeroLoans() {
            // Given: Tool1 con préstamos, Tool2 y Tool3 sin préstamos
            createLoan(client1, tool1, LocalDate.now());
            createLoan(client2, tool1, LocalDate.now().minusDays(1));

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedTools(PageRequest.of(0, 10));

            // Then: Todas las herramientas aparecen
            assertThat(result).hasSize(3);

            // Tool1 con 2 préstamos
            assertThat(result.get(0).getToolId()).isEqualTo(tool1.getId());
            assertThat(result.get(0).getLoanCount()).isEqualTo(2L);

            // Tool2 y Tool3 con 0 préstamos (orden puede variar)
            assertThat(result.get(1).getLoanCount()).isEqualTo(0L);
            assertThat(result.get(2).getLoanCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("RF6.3: Debe contar préstamos devueltos y activos")
        void findMostLoanedTools_countsAllLoans() {
            // Given: Préstamos activos y devueltos
            LoanEntity active = createLoan(client1, tool1, LocalDate.now());

            LoanEntity returned = createLoan(client2, tool1, LocalDate.now().minusDays(5));
            returned.setReturnDate(LocalDate.now().minusDays(2));
            returned.setStatus("Devuelto");
            entityManager.persist(returned);

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedTools(PageRequest.of(0, 10));

            // Then: Cuenta ambos préstamos
            assertThat(result.get(0).getToolId()).isEqualTo(tool1.getId());
            assertThat(result.get(0).getLoanCount()).isEqualTo(2L); // Activo + Devuelto
        }

        @Test
        @DisplayName("RF6.3: Debe manejar empates en cantidad de préstamos")
        void findMostLoanedTools_handlesTies() {
            // Given: Tool1 y Tool2 con misma cantidad
            createLoan(client1, tool1, LocalDate.now());
            createLoan(client2, tool1, LocalDate.now().minusDays(1));

            createLoan(client1, tool2, LocalDate.now());
            createLoan(client2, tool2, LocalDate.now().minusDays(1));

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedTools(PageRequest.of(0, 10));

            // Then: Ambas tienen count = 2
            assertThat(result.get(0).getLoanCount()).isEqualTo(2L);
            assertThat(result.get(1).getLoanCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Tests de findMostLoanedToolsByDateRange() - RF6.3")
    class FindMostLoanedToolsByDateRangeTests {

        @Test
        @DisplayName("RF6.3: Debe retornar solo préstamos dentro del rango de fechas")
        void findMostLoanedToolsByDateRange_filtersByDateRange() {
            // Given: Préstamos en diferentes fechas
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            // Dentro del rango
            createLoan(client1, tool1, LocalDate.of(2025, 1, 10));
            createLoan(client2, tool1, LocalDate.of(2025, 1, 20));
            createLoan(client1, tool2, LocalDate.of(2025, 1, 15));

            // Fuera del rango
            createLoan(client2, tool1, LocalDate.of(2024, 12, 25));
            createLoan(client1, tool3, LocalDate.of(2025, 2, 5));

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            startRange, endRange, PageRequest.of(0, 10)
                    );

            // Then: Tool1 (2 en rango), Tool2 (1 en rango), Tool3 (0 en rango)
            assertThat(result).hasSize(3);

            assertThat(result.get(0).getToolId()).isEqualTo(tool1.getId());
            assertThat(result.get(0).getLoanCount()).isEqualTo(2L);

            assertThat(result.get(1).getToolId()).isEqualTo(tool2.getId());
            assertThat(result.get(1).getLoanCount()).isEqualTo(1L);

            // Tool3 con 0 préstamos en el rango
            ReportController.ToolRanking tool3Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool3.getId()))
                    .findFirst()
                    .orElse(null);
            assertThat(tool3Ranking).isNotNull();
            assertThat(tool3Ranking.getLoanCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("RF6.3: Debe ordenar por cantidad descendente")
        void findMostLoanedToolsByDateRange_ordersByCountDesc() {
            // Given: Préstamos en el rango
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            // Tool1: 4 préstamos
            for (int i = 0; i < 4; i++) {
                createLoan(client1, tool1, LocalDate.of(2025, 1, 5 + i));
            }

            // Tool2: 2 préstamos
            createLoan(client2, tool2, LocalDate.of(2025, 1, 10));
            createLoan(client1, tool2, LocalDate.of(2025, 1, 15));

            // Tool3: 1 préstamo
            createLoan(client2, tool3, LocalDate.of(2025, 1, 20));

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            startRange, endRange, PageRequest.of(0, 10)
                    );

            // Then: Tool1 (4), Tool2 (2), Tool3 (1)
            assertThat(result.get(0).getToolId()).isEqualTo(tool1.getId());
            assertThat(result.get(0).getLoanCount()).isEqualTo(4L);

            assertThat(result.get(1).getToolId()).isEqualTo(tool2.getId());
            assertThat(result.get(1).getLoanCount()).isEqualTo(2L);

            assertThat(result.get(2).getToolId()).isEqualTo(tool3.getId());
            assertThat(result.get(2).getLoanCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("RF6.3: Debe respetar el límite (Pageable)")
        void findMostLoanedToolsByDateRange_respectsLimit() {
            // Given: 3 herramientas con préstamos en el rango
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            createLoan(client1, tool1, LocalDate.of(2025, 1, 10));
            createLoan(client1, tool2, LocalDate.of(2025, 1, 15));
            createLoan(client1, tool3, LocalDate.of(2025, 1, 20));

            entityManager.flush();

            // When: Solicitar top 2
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            startRange, endRange, PageRequest.of(0, 2)
                    );

            // Then: Solo 2 resultados
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("RF6.3: Debe incluir fechas límite (startDate y endDate)")
        void findMostLoanedToolsByDateRange_includesBoundaryDates() {
            // Given: Préstamos en las fechas límite
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            createLoan(client1, tool1, startRange); // En el límite inferior
            createLoan(client2, tool2, endRange);   // En el límite superior

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            startRange, endRange, PageRequest.of(0, 10)
                    );

            // Then: Ambos préstamos deben contarse
            ReportController.ToolRanking tool1Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool1.getId()))
                    .findFirst()
                    .orElse(null);

            ReportController.ToolRanking tool2Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool2.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(tool1Ranking).isNotNull();
            assertThat(tool1Ranking.getLoanCount()).isEqualTo(1L);

            assertThat(tool2Ranking).isNotNull();
            assertThat(tool2Ranking.getLoanCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("RF6.3: Debe retornar todas las herramientas con count = 0 si no hay préstamos en el rango")
        void findMostLoanedToolsByDateRange_returnsAllToolsWithZeroWhenNoLoansInRange() {
            // Given: Préstamos fuera del rango
            createLoan(client1, tool1, LocalDate.of(2024, 12, 15));

            entityManager.flush();

            // When: Buscar en rango donde no hay préstamos
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            LocalDate.of(2025, 1, 1),
                            LocalDate.of(2025, 1, 31),
                            PageRequest.of(0, 10)
                    );

            // Then: Todas las herramientas con count = 0
            assertThat(result).hasSize(3);
            assertThat(result).allMatch(r -> r.getLoanCount() == 0L);
        }

        @Test
        @DisplayName("RF6.3: Debe funcionar con rangos de un solo día")
        void findMostLoanedToolsByDateRange_worksWithSingleDay() {
            // Given: Préstamos en un día específico
            LocalDate targetDate = LocalDate.of(2025, 1, 15);

            createLoan(client1, tool1, targetDate);
            createLoan(client2, tool1, targetDate);
            createLoan(client1, tool2, LocalDate.of(2025, 1, 14)); // Día anterior

            entityManager.flush();

            // When: Buscar solo en ese día
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            targetDate, targetDate, PageRequest.of(0, 10)
                    );

            // Then: Solo cuenta préstamos de ese día
            ReportController.ToolRanking tool1Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool1.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(tool1Ranking).isNotNull();
            assertThat(tool1Ranking.getLoanCount()).isEqualTo(2L);

            // Tool2 no tiene préstamos en ese día
            ReportController.ToolRanking tool2Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool2.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(tool2Ranking).isNotNull();
            assertThat(tool2Ranking.getLoanCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("RF6.3: Debe contar correctamente préstamos devueltos y activos dentro del rango")
        void findMostLoanedToolsByDateRange_countsAllLoansInRange() {
            // Given: Préstamos activos y devueltos en el rango
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            // Activo
            createLoan(client1, tool1, LocalDate.of(2025, 1, 10));

            // Devuelto
            LoanEntity returned = createLoan(client2, tool1, LocalDate.of(2025, 1, 20));
            returned.setReturnDate(LocalDate.of(2025, 1, 25));
            returned.setStatus("Devuelto");
            entityManager.persist(returned);

            entityManager.flush();

            // When
            List<ReportController.ToolRanking> result =
                    toolRepository.findMostLoanedToolsByDateRange(
                            startRange, endRange, PageRequest.of(0, 10)
                    );

            // Then: Debe contar ambos
            ReportController.ToolRanking tool1Ranking = result.stream()
                    .filter(r -> r.getToolId().equals(tool1.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(tool1Ranking).isNotNull();
            assertThat(tool1Ranking.getLoanCount()).isEqualTo(2L);
        }
    }

    // ========== MÉTODOS HELPER ==========

    private LoanEntity createLoan(ClientEntity client, ToolEntity tool, LocalDate startDate) {
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(startDate);
        loan.setDueDate(startDate.plusDays(7));
        loan.setReturnDate(null);
        loan.setStatus("Vigente");
        loan.setFine(0.0);
        loan.setRentalCost(5000.0);
        loan.setDamaged(false);
        loan.setIrreparable(false);
        return entityManager.persist(loan);
    }
}