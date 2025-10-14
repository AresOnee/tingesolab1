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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ClientEntity client1;
    private ClientEntity client2;
    private ToolEntity tool1;
    private ToolEntity tool2;

    @BeforeEach
    void setUp() {
        // Preparamos clientes de prueba
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

        // Preparamos herramientas de prueba
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

    // ========== TESTS ORIGINALES ==========

    @Nested
    class CountActiveByClientIdTests {

        @Test
        @DisplayName("Cuenta correctamente préstamos activos (Vigente y Atrasado)")
        void countActive_shouldCountVigenteAndAtrasado() {
            // Given: Cliente con 2 préstamos vigentes y 1 atrasado
            createLoan(client1, tool1, "Vigente", LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), null);
            createLoan(client1, tool2, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), null);
            createLoan(client1, tool1, "Atrasado", LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), null);

            entityManager.flush();

            // When: Contamos los préstamos activos
            long count = loanRepository.countActiveByClientId(client1.getId());

            // Then: Debe contar los 3 préstamos (2 Vigente + 1 Atrasado)
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("No cuenta préstamos devueltos")
        void countActive_shouldNotCountDevuelto() {
            // Given: Cliente con 1 préstamo vigente y 1 devuelto
            createLoan(client1, tool1, "Vigente", LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), null);
            createLoan(client1, tool2, "Devuelto", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));

            entityManager.flush();

            // When: Contamos los activos
            long count = loanRepository.countActiveByClientId(client1.getId());

            // Then: Solo debe contar el vigente
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Retorna 0 cuando el cliente no tiene préstamos")
        void countActive_shouldReturnZeroWhenNoLoans() {
            // Given: Cliente sin préstamos

            // When: Contamos
            long count = loanRepository.countActiveByClientId(client1.getId());

            // Then: Debe ser 0
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Solo cuenta préstamos del cliente específico")
        void countActive_shouldCountOnlyForSpecificClient() {
            // Given: Préstamos de diferentes clientes
            createLoan(client1, tool1, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), null);
            createLoan(client1, tool2, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(3), null);
            createLoan(client2, tool1, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(7), null);

            entityManager.flush();

            // When: Contamos para client1
            long count = loanRepository.countActiveByClientId(client1.getId());

            // Then: Solo debe contar los 2 de client1
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Tests de existsActiveByClientAndTool")
    class ExistsActiveByClientAndToolTests {

        @Test
        @DisplayName("Retorna true cuando existe préstamo activo de esa herramienta")
        void existsActive_shouldReturnTrueWhenActiveExists() {
            // Given: Cliente con préstamo vigente de tool1
            createLoan(client1, tool1, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), null);
            entityManager.flush();

            // When: Verificamos si existe
            boolean exists = loanRepository.existsActiveByClientAndTool(client1.getId(), tool1.getId());

            // Then: Debe ser true
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Retorna true para préstamos atrasados (también son activos)")
        void existsActive_shouldReturnTrueForAtrasado() {
            // Given: Cliente con préstamo atrasado de tool1
            createLoan(client1, tool1, "Atrasado", LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), null);
            entityManager.flush();

            // When: Verificamos
            boolean exists = loanRepository.existsActiveByClientAndTool(client1.getId(), tool1.getId());

            // Then: Debe ser true porque Atrasado cuenta como activo
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Retorna false cuando el préstamo está devuelto")
        void existsActive_shouldReturnFalseWhenDevuelto() {
            // Given: Cliente con préstamo devuelto de tool1
            createLoan(client1, tool1, "Devuelto", LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), LocalDate.now().minusDays(1));
            entityManager.flush();

            // When: Verificamos
            boolean exists = loanRepository.existsActiveByClientAndTool(client1.getId(), tool1.getId());

            // Then: Debe ser false
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Retorna false cuando no existe ningún préstamo")
        void existsActive_shouldReturnFalseWhenNoLoans() {
            // Given: Sin préstamos

            // When: Verificamos
            boolean exists = loanRepository.existsActiveByClientAndTool(client1.getId(), tool1.getId());

            // Then: Debe ser false
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Distingue correctamente entre diferentes herramientas")
        void existsActive_shouldDistinguishBetweenTools() {
            // Given: Cliente con préstamo de tool1 pero no de tool2
            createLoan(client1, tool1, "Vigente", LocalDate.now().minusDays(1), LocalDate.now().plusDays(5), null);
            entityManager.flush();

            // When: Verificamos para ambas herramientas
            boolean existsTool1 = loanRepository.existsActiveByClientAndTool(client1.getId(), tool1.getId());
            boolean existsTool2 = loanRepository.existsActiveByClientAndTool(client1.getId(), tool2.getId());

            // Then: Solo debe existir para tool1
            assertThat(existsTool1).isTrue();
            assertThat(existsTool2).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests de hasOverduesOrFines")
    class HasOverduesOrFinesTests {

        @Test
        @DisplayName("Retorna true cuando el cliente tiene préstamos atrasados")
        void hasOverdues_shouldReturnTrueWhenAtrasado() {
            // Given: Cliente con préstamo atrasado
            createLoanWithFine(client1, tool1, "Atrasado", 0.0);
            entityManager.flush();

            // When: Verificamos
            boolean hasIssues = loanRepository.hasOverduesOrFines(client1.getId());

            // Then: Debe ser true
            assertThat(hasIssues).isTrue();
        }

        @Test
        @DisplayName("Retorna true cuando el cliente tiene multas pendientes")
        void hasOverdues_shouldReturnTrueWhenHasFines() {
            // Given: Cliente con préstamo vigente pero con multa
            createLoanWithFine(client1, tool1, "Vigente", 5000.0);
            entityManager.flush();

            // When: Verificamos
            boolean hasIssues = loanRepository.hasOverduesOrFines(client1.getId());

            // Then: Debe ser true porque tiene multa > 0
            assertThat(hasIssues).isTrue();
        }

        @Test
        @DisplayName("Retorna false cuando todos los préstamos están vigentes sin multas")
        void hasOverdues_shouldReturnFalseWhenNoIssues() {
            // Given: Cliente con préstamo vigente sin multas
            createLoanWithFine(client1, tool1, "Vigente", 0.0);
            entityManager.flush();

            // When: Verificamos
            boolean hasIssues = loanRepository.hasOverduesOrFines(client1.getId());

            // Then: Debe ser false
            assertThat(hasIssues).isFalse();
        }

        @Test
        @DisplayName("Retorna false cuando el cliente no tiene préstamos")
        void hasOverdues_shouldReturnFalseWhenNoLoans() {
            // Given: Cliente sin préstamos

            // When: Verificamos
            boolean hasIssues = loanRepository.hasOverduesOrFines(client1.getId());

            // Then: Debe ser false
            assertThat(hasIssues).isFalse();
        }
    }

    @Test
    @DisplayName("Test básico: guardar y encontrar préstamo")
    void saveAndFind() {
        // Given: Un préstamo válido
        LoanEntity loan = createLoan(client1, tool1, "Vigente",
                LocalDate.now(), LocalDate.now().plusDays(7), null);
        entityManager.flush();

        // When: Lo buscamos por ID
        LoanEntity found = loanRepository.findById(loan.getId()).orElse(null);

        // Then: Debe existir y tener los datos correctos
        assertThat(found).isNotNull();
        assertThat(found.getClient().getId()).isEqualTo(client1.getId());
        assertThat(found.getTool().getId()).isEqualTo(tool1.getId());
        assertThat(found.getStatus()).isEqualTo("Vigente");
    }

    // ========== NUEVOS TESTS ÉPICA 6 (RF6.1) ==========

    @Nested
    @DisplayName("Tests de findActiveLoans() - RF6.1")
    class FindActiveLoansTests {

        @Test
        @DisplayName("RF6.1: Debe retornar préstamos sin devolver (returnDate = null)")
        void findActiveLoans_returnsOnlyUnreturnedLoans() {
            // Given: 2 préstamos activos, 1 devuelto
            LoanEntity active1 = createLoan(client1, tool1, "Vigente",
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), null);

            LoanEntity active2 = createLoan(client2, tool2, "Atrasado",
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), null);

            LoanEntity returned = createLoan(client1, tool2, "Devuelto",
                    LocalDate.now().minusDays(7), LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(1));

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoans();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(active1, active2);
            assertThat(result).doesNotContain(returned);
        }

        @Test
        @DisplayName("RF6.1: Debe retornar préstamos vigentes y atrasados")
        void findActiveLoans_includesBothVigenteAndAtrasado() {
            // Given: 1 vigente, 1 atrasado
            LoanEntity vigente = createLoan(client1, tool1, "Vigente",
                    LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), null);

            LoanEntity atrasado = createLoan(client2, tool2, "Atrasado",
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoans();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(vigente, atrasado);
        }

        @Test
        @DisplayName("RF6.1: Debe ordenar por dueDate ascendente (más urgentes primero)")
        void findActiveLoans_ordersByDueDateAsc() {
            // Given: 3 préstamos con diferentes dueDates
            LoanEntity loan1 = createLoan(client1, tool1, "Vigente",
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(10), null);

            LoanEntity loan2 = createLoan(client2, tool2, "Vigente",
                    LocalDate.now().minusDays(3), LocalDate.now().plusDays(2), null);

            LoanEntity loan3 = createLoan(client1, tool2, "Atrasado",
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoans();

            // Then
            assertThat(result).hasSize(3);
            // Orden esperado: loan3 (vencido), loan2 (+2 días), loan1 (+10 días)
            assertThat(result.get(0)).isEqualTo(loan3);
            assertThat(result.get(1)).isEqualTo(loan2);
            assertThat(result.get(2)).isEqualTo(loan1);
        }

        @Test
        @DisplayName("RF6.1: Debe retornar lista vacía si no hay préstamos activos")
        void findActiveLoans_returnsEmptyListWhenNoActiveLoans() {
            // Given: Solo préstamos devueltos
            createLoan(client1, tool1, "Devuelto",
                    LocalDate.now().minusDays(7), LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(1));

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoans();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.1: Debe retornar lista vacía si no hay préstamos en la BD")
        void findActiveLoans_returnsEmptyListWhenNoLoans() {
            // When
            List<LoanEntity> result = loanRepository.findActiveLoans();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests de findActiveLoansByDateRange() - RF6.1")
    class FindActiveLoansByDateRangeTests {

        @Test
        @DisplayName("RF6.1: Debe retornar solo préstamos dentro del rango de fechas")
        void findActiveLoansByDateRange_returnsLoansInRange() {
            // Given: Préstamos en diferentes fechas
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            // Dentro del rango
            LoanEntity inRange1 = createLoan(client1, tool1, "Vigente",
                    LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20), null);

            LoanEntity inRange2 = createLoan(client2, tool2, "Vigente",
                    LocalDate.of(2025, 1, 25), LocalDate.of(2025, 2, 5), null);

            // Fuera del rango
            LoanEntity outOfRange = createLoan(client1, tool2, "Vigente",
                    LocalDate.of(2024, 12, 15), LocalDate.of(2024, 12, 30), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoansByDateRange(
                    startRange, endRange
            );

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(inRange1, inRange2);
            assertThat(result).doesNotContain(outOfRange);
        }

        @Test
        @DisplayName("RF6.1: Debe excluir préstamos devueltos aunque estén en rango de fechas")
        void findActiveLoansByDateRange_excludesReturnedLoans() {
            // Given: 1 activo, 1 devuelto en el mismo rango
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            LoanEntity active = createLoan(client1, tool1, "Vigente",
                    LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20), null);

            LoanEntity returned = createLoan(client2, tool2, "Devuelto",
                    LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 25),
                    LocalDate.of(2025, 1, 23));

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoansByDateRange(
                    startRange, endRange
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsOnly(active);
            assertThat(result).doesNotContain(returned);
        }

        @Test
        @DisplayName("RF6.1: Debe ordenar por dueDate ascendente")
        void findActiveLoansByDateRange_ordersByDueDateAsc() {
            // Given: Préstamos con diferentes dueDates en el rango
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            LoanEntity loan1 = createLoan(client1, tool1, "Vigente",
                    LocalDate.of(2025, 1, 5), LocalDate.of(2025, 2, 15), null);

            LoanEntity loan2 = createLoan(client2, tool2, "Vigente",
                    LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 25), null);

            LoanEntity loan3 = createLoan(client1, tool2, "Vigente",
                    LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 1), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoansByDateRange(
                    startRange, endRange
            );

            // Then
            assertThat(result).hasSize(3);
            // Orden: loan2 (1/25), loan3 (2/1), loan1 (2/15)
            assertThat(result.get(0)).isEqualTo(loan2);
            assertThat(result.get(1)).isEqualTo(loan3);
            assertThat(result.get(2)).isEqualTo(loan1);
        }

        @Test
        @DisplayName("RF6.1: Debe retornar lista vacía si no hay préstamos en el rango")
        void findActiveLoansByDateRange_returnsEmptyListWhenNoLoansInRange() {
            // Given: Préstamo fuera del rango
            createLoan(client1, tool1, "Vigente",
                    LocalDate.of(2024, 12, 15), LocalDate.of(2024, 12, 30), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoansByDateRange(
                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
            );

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RF6.1: Debe incluir préstamos en los límites del rango (startDate y endDate)")
        void findActiveLoansByDateRange_includesBoundaryDates() {
            // Given: Préstamos en las fechas límite
            LocalDate startRange = LocalDate.of(2025, 1, 1);
            LocalDate endRange = LocalDate.of(2025, 1, 31);

            LoanEntity onStartDate = createLoan(client1, tool1, "Vigente",
                    startRange, LocalDate.of(2025, 1, 10), null);

            LoanEntity onEndDate = createLoan(client2, tool2, "Vigente",
                    endRange, LocalDate.of(2025, 2, 5), null);

            entityManager.flush();

            // When
            List<LoanEntity> result = loanRepository.findActiveLoansByDateRange(
                    startRange, endRange
            );

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(onStartDate, onEndDate);
        }
    }

    // ========== MÉTODOS HELPER ==========

    private LoanEntity createLoan(ClientEntity client, ToolEntity tool, String status,
                                  LocalDate startDate, LocalDate dueDate, LocalDate returnDate) {
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(startDate);
        loan.setDueDate(dueDate);
        loan.setReturnDate(returnDate);
        loan.setStatus(status);
        loan.setFine(0.0);
        loan.setRentalCost(5000.0);
        loan.setDamaged(false);
        loan.setIrreparable(false);
        return entityManager.persist(loan);
    }

    private LoanEntity createLoanWithFine(ClientEntity client, ToolEntity tool,
                                          String status, Double fine) {
        LoanEntity loan = new LoanEntity();
        loan.setClient(client);
        loan.setTool(tool);
        loan.setStartDate(LocalDate.now().minusDays(5));
        loan.setDueDate(LocalDate.now().plusDays(2));
        loan.setReturnDate(null);
        loan.setStatus(status);
        loan.setFine(fine);
        loan.setRentalCost(5000.0);
        loan.setDamaged(false);
        loan.setIrreparable(false);
        return entityManager.persist(loan);
    }
}