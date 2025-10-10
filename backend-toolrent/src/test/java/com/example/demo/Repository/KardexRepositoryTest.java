package com.example.demo.Repository;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Entity.ToolEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("KardexRepository Tests")
class KardexRepositoryTest {

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private EntityManager entityManager;

    private ToolEntity tool1;
    private ToolEntity tool2;

    @BeforeEach
    void setUp() {
        // Limpiar datos previos
        kardexRepository.deleteAll();
        toolRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Crear herramientas de prueba
        tool1 = new ToolEntity();
        tool1.setName("Taladro Test");
        tool1.setCategory("Electricas");
        tool1.setReplacementValue(50000);
        tool1.setStock(5);
        tool1.setStatus("Disponible");
        tool1 = toolRepository.save(tool1);

        tool2 = new ToolEntity();
        tool2.setName("Sierra Test");
        tool2.setCategory("Manuales");
        tool2.setReplacementValue(30000);
        tool2.setStock(3);
        tool2.setStatus("Disponible");
        tool2 = toolRepository.save(tool2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByToolOrderByMovementDateDesc: debe retornar movimientos de una herramienta ordenados por fecha desc")
    void findByToolOrderByMovementDateDesc_returnsMovementsOrderedByDateDesc() {
        // Given
        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10));
        KardexEntity k2 = createKardex(tool1, "PRESTAMO", -1, LocalDateTime.now().minusDays(5));
        KardexEntity k3 = createKardex(tool1, "DEVOLUCION", 1, LocalDateTime.now().minusDays(1));
        KardexEntity k4 = createKardex(tool2, "REGISTRO", 3, LocalDateTime.now().minusDays(3));
        kardexRepository.saveAll(List.of(k1, k2, k3, k4));
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findByToolOrderByMovementDateDesc(tool1);

        // Then
        assertThat(movements).hasSize(3);
        assertThat(movements.get(0).getMovementType()).isEqualTo("DEVOLUCION"); // más reciente
        assertThat(movements.get(1).getMovementType()).isEqualTo("PRESTAMO");
        assertThat(movements.get(2).getMovementType()).isEqualTo("REGISTRO"); // más antiguo
        assertThat(movements).allMatch(k -> k.getTool().getId().equals(tool1.getId()));
    }

    @Test
    @DisplayName("findByToolOrderByMovementDateDesc: debe retornar lista vacía si no hay movimientos para la herramienta")
    void findByToolOrderByMovementDateDesc_returnsEmptyListWhenNoMovements() {
        // Given: herramienta sin movimientos
        // When
        List<KardexEntity> movements = kardexRepository.findByToolOrderByMovementDateDesc(tool1);

        // Then
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("findByMovementDateBetweenOrderByMovementDateDesc: debe filtrar por rango de fechas")
    void findByMovementDateBetweenOrderByMovementDateDesc_filtersCorrectly() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now().minusDays(2);

        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10)); // fuera del rango
        KardexEntity k2 = createKardex(tool1, "PRESTAMO", -1, LocalDateTime.now().minusDays(5)); // dentro del rango
        KardexEntity k3 = createKardex(tool1, "DEVOLUCION", 1, LocalDateTime.now().minusDays(3)); // dentro del rango
        KardexEntity k4 = createKardex(tool2, "REGISTRO", 3, LocalDateTime.now().minusDays(1)); // fuera del rango
        kardexRepository.saveAll(List.of(k1, k2, k3, k4));
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findByMovementDateBetweenOrderByMovementDateDesc(start, end);

        // Then
        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).getMovementType()).isEqualTo("DEVOLUCION"); // más reciente primero
        assertThat(movements.get(1).getMovementType()).isEqualTo("PRESTAMO");
    }

    @Test
    @DisplayName("findByMovementDateBetweenOrderByMovementDateDesc: debe retornar lista vacía si no hay movimientos en el rango")
    void findByMovementDateBetweenOrderByMovementDateDesc_returnsEmptyWhenNoMovementsInRange() {
        // Given
        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10));
        kardexRepository.save(k1);
        entityManager.flush();

        LocalDateTime start = LocalDateTime.now().minusDays(5);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        // When
        List<KardexEntity> movements = kardexRepository.findByMovementDateBetweenOrderByMovementDateDesc(start, end);

        // Then
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("findAllByOrderByMovementDateDesc: debe retornar todos los movimientos ordenados por fecha desc")
    void findAllByOrderByMovementDateDesc_returnsAllMovementsOrderedByDateDesc() {
        // Given
        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10));
        KardexEntity k2 = createKardex(tool2, "PRESTAMO", -1, LocalDateTime.now().minusDays(5));
        KardexEntity k3 = createKardex(tool1, "DEVOLUCION", 1, LocalDateTime.now().minusDays(1));
        kardexRepository.saveAll(List.of(k1, k2, k3));
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findAllByOrderByMovementDateDesc();

        // Then
        assertThat(movements).hasSize(3);
        assertThat(movements.get(0).getMovementType()).isEqualTo("DEVOLUCION"); // más reciente
        assertThat(movements.get(1).getMovementType()).isEqualTo("PRESTAMO");
        assertThat(movements.get(2).getMovementType()).isEqualTo("REGISTRO"); // más antiguo
    }

    @Test
    @DisplayName("findByMovementTypeOrderByMovementDateDesc: debe filtrar por tipo de movimiento")
    void findByMovementTypeOrderByMovementDateDesc_filtersCorrectly() {
        // Given
        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10));
        KardexEntity k2 = createKardex(tool1, "PRESTAMO", -1, LocalDateTime.now().minusDays(5));
        KardexEntity k3 = createKardex(tool2, "PRESTAMO", -1, LocalDateTime.now().minusDays(3));
        KardexEntity k4 = createKardex(tool1, "DEVOLUCION", 1, LocalDateTime.now().minusDays(1));
        kardexRepository.saveAll(List.of(k1, k2, k3, k4));
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findByMovementTypeOrderByMovementDateDesc("PRESTAMO");

        // Then
        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(k -> k.getMovementType().equals("PRESTAMO"));
        assertThat(movements.get(0).getMovementDate()).isAfter(movements.get(1).getMovementDate());
    }

    @Test
    @DisplayName("findByMovementTypeOrderByMovementDateDesc: debe retornar lista vacía si no hay movimientos del tipo")
    void findByMovementTypeOrderByMovementDateDesc_returnsEmptyWhenNoMovementsOfType() {
        // Given
        KardexEntity k1 = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now().minusDays(10));
        kardexRepository.save(k1);
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findByMovementTypeOrderByMovementDateDesc("BAJA");

        // Then
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("findByToolAndMovementTypeOrderByMovementDateDesc: debe filtrar por herramienta y tipo")
    void findByToolAndMovementTypeOrderByMovementDateDesc_filtersCorrectly() {
        // Given
        KardexEntity k1 = createKardex(tool1, "PRESTAMO", -1, LocalDateTime.now().minusDays(10));
        KardexEntity k2 = createKardex(tool1, "PRESTAMO", -1, LocalDateTime.now().minusDays(5));
        KardexEntity k3 = createKardex(tool2, "PRESTAMO", -1, LocalDateTime.now().minusDays(3));
        KardexEntity k4 = createKardex(tool1, "DEVOLUCION", 1, LocalDateTime.now().minusDays(1));
        kardexRepository.saveAll(List.of(k1, k2, k3, k4));
        entityManager.flush();

        // When
        List<KardexEntity> movements = kardexRepository.findByToolAndMovementTypeOrderByMovementDateDesc(tool1, "PRESTAMO");

        // Then
        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(k ->
                k.getTool().getId().equals(tool1.getId()) &&
                        k.getMovementType().equals("PRESTAMO")
        );
        assertThat(movements.get(0).getMovementDate()).isAfter(movements.get(1).getMovementDate());
    }

    @Test
    @DisplayName("save: debe guardar movimiento correctamente con todos los campos")
    void save_savesMovementWithAllFields() {
        // Given
        KardexEntity kardex = new KardexEntity();
        kardex.setMovementType("PRESTAMO");
        kardex.setTool(tool1);
        kardex.setQuantity(-1);
        kardex.setUsername("TEST_USER");
        kardex.setMovementDate(LocalDateTime.now());
        kardex.setObservations("Test observation");
        kardex.setLoanId(100L);

        // When
        KardexEntity saved = kardexRepository.save(kardex);
        entityManager.flush();
        entityManager.clear();

        // Then
        KardexEntity found = kardexRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getMovementType()).isEqualTo("PRESTAMO");
        assertThat(found.getTool().getId()).isEqualTo(tool1.getId());
        assertThat(found.getQuantity()).isEqualTo(-1);
        assertThat(found.getUsername()).isEqualTo("TEST_USER");
        assertThat(found.getMovementDate()).isNotNull();
        assertThat(found.getObservations()).isEqualTo("Test observation");
        assertThat(found.getLoanId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("delete: debe eliminar movimiento correctamente")
    void delete_removesMovement() {
        // Given
        KardexEntity kardex = createKardex(tool1, "REGISTRO", 5, LocalDateTime.now());
        KardexEntity saved = kardexRepository.save(kardex);
        entityManager.flush();
        Long id = saved.getId();

        // When
        kardexRepository.delete(saved);
        entityManager.flush();

        // Then
        assertThat(kardexRepository.findById(id)).isEmpty();
    }

    // ===== HELPER METHODS =====

    private KardexEntity createKardex(ToolEntity tool, String type, Integer qty, LocalDateTime date) {
        KardexEntity k = new KardexEntity();
        k.setTool(tool);
        k.setMovementType(type);
        k.setQuantity(qty);
        k.setUsername("TEST_USER");
        k.setMovementDate(date);
        k.setObservations("Test movement");
        k.setLoanId(null);
        return k;
    }
}