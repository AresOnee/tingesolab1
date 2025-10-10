package com.example.demo.Service;

import com.example.demo.Entity.KardexEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.KardexRepository;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KardexService Tests")
class KardexServiceTest {

    @Mock
    private KardexRepository kardexRepository;

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private KardexService kardexService;

    private ToolEntity tool;

    @BeforeEach
    void setUp() {
        tool = new ToolEntity();
        tool.setId(1L);
        tool.setName("Taladro");
        tool.setCategory("Electricas");
        tool.setReplacementValue(50000);
        tool.setStock(5);
        tool.setStatus("Disponible");
    }

    // ===== TESTS PARA registerMovement =====

    @Test
    @DisplayName("registerMovement: debe crear movimiento correctamente con todos los campos")
    void registerMovement_success() {
        // Given
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(kardexRepository.save(any(KardexEntity.class))).thenAnswer(inv -> {
            KardexEntity k = inv.getArgument(0);
            k.setId(100L);
            return k;
        });

        // When
        KardexEntity result = kardexService.registerMovement(
                1L, "REGISTRO", 5, "ADMIN", "Alta inicial", null
        );

        // Then
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getMovementType()).isEqualTo("REGISTRO");
        assertThat(result.getQuantity()).isEqualTo(5);
        assertThat(result.getUsername()).isEqualTo("ADMIN");
        assertThat(result.getObservations()).isEqualTo("Alta inicial");
        assertThat(result.getLoanId()).isNull();
        assertThat(result.getMovementDate()).isNotNull();
        verify(toolRepository).findById(1L);
        verify(kardexRepository).save(any(KardexEntity.class));
    }

    @Test
    @DisplayName("registerMovement: debe convertir movementType a mayúsculas")
    void registerMovement_convertsMovementTypeToUppercase() {
        // Given
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(kardexRepository.save(any(KardexEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        KardexEntity result = kardexService.registerMovement(
                1L, "prestamo", -1, "USER", "Test", 10L
        );

        // Then
        assertThat(result.getMovementType()).isEqualTo("PRESTAMO");
    }

    @Test
    @DisplayName("registerMovement: debe usar 'SYSTEM' si username es null")
    void registerMovement_usesSystemWhenUsernameIsNull() {
        // Given
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(kardexRepository.save(any(KardexEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        KardexEntity result = kardexService.registerMovement(
                1L, "REGISTRO", 5, null, "Test", null
        );

        // Then
        assertThat(result.getUsername()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("registerMovement: debe fallar si toolId es null")
    void registerMovement_toolIdNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.registerMovement(
                null, "REGISTRO", 5, "ADMIN", "Test", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ID de herramienta es obligatorio");

        verify(toolRepository, never()).findById(any());
        verify(kardexRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerMovement: debe fallar si movementType es null")
    void registerMovement_movementTypeNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.registerMovement(
                1L, null, 5, "ADMIN", "Test", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tipo de movimiento es obligatorio");

        verify(kardexRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerMovement: debe fallar si movementType es blank")
    void registerMovement_movementTypeBlank_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.registerMovement(
                1L, "   ", 5, "ADMIN", "Test", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tipo de movimiento es obligatorio");

        verify(kardexRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerMovement: debe fallar si quantity es null")
    void registerMovement_quantityNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.registerMovement(
                1L, "REGISTRO", null, "ADMIN", "Test", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cantidad es obligatoria");

        verify(kardexRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerMovement: debe fallar si herramienta no existe")
    void registerMovement_toolNotFound_throwsNotFound() {
        // Given
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kardexService.registerMovement(
                999L, "REGISTRO", 5, "ADMIN", "Test", null
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Herramienta no encontrada con ID: 999");

        verify(toolRepository).findById(999L);
        verify(kardexRepository, never()).save(any());
    }

    // ===== TESTS PARA getMovementsByTool =====

    @Test
    @DisplayName("getMovementsByTool: debe retornar movimientos de la herramienta")
    void getMovementsByTool_success() {
        // Given
        KardexEntity k1 = new KardexEntity();
        k1.setId(1L);
        k1.setMovementType("REGISTRO");
        k1.setTool(tool);

        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(kardexRepository.findByToolOrderByMovementDateDesc(tool)).thenReturn(List.of(k1));

        // When
        List<KardexEntity> result = kardexService.getMovementsByTool(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMovementType()).isEqualTo("REGISTRO");
        verify(toolRepository).findById(1L);
        verify(kardexRepository).findByToolOrderByMovementDateDesc(tool);
    }

    @Test
    @DisplayName("getMovementsByTool: debe retornar lista vacía si no hay movimientos")
    void getMovementsByTool_returnsEmptyListWhenNoMovements() {
        // Given
        when(toolRepository.findById(1L)).thenReturn(Optional.of(tool));
        when(kardexRepository.findByToolOrderByMovementDateDesc(tool)).thenReturn(List.of());

        // When
        List<KardexEntity> result = kardexService.getMovementsByTool(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getMovementsByTool: debe fallar si toolId es null")
    void getMovementsByTool_toolIdNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByTool(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ID de herramienta es obligatorio");

        verify(toolRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getMovementsByTool: debe fallar si herramienta no existe")
    void getMovementsByTool_toolNotFound_throwsNotFound() {
        // Given
        when(toolRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByTool(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Herramienta no encontrada con ID: 999");

        verify(toolRepository).findById(999L);
        verify(kardexRepository, never()).findByToolOrderByMovementDateDesc(any());
    }

    // ===== TESTS PARA getMovementsByDateRange =====

    @Test
    @DisplayName("getMovementsByDateRange: debe retornar movimientos en el rango de fechas")
    void getMovementsByDateRange_success() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        KardexEntity k1 = new KardexEntity();
        k1.setId(1L);
        k1.setMovementType("PRESTAMO");

        when(kardexRepository.findByMovementDateBetweenOrderByMovementDateDesc(any(), any()))
                .thenReturn(List.of(k1));

        // When
        List<KardexEntity> result = kardexService.getMovementsByDateRange(startDate, endDate);

        // Then
        assertThat(result).hasSize(1);
        verify(kardexRepository).findByMovementDateBetweenOrderByMovementDateDesc(any(), any());
    }

    @Test
    @DisplayName("getMovementsByDateRange: debe fallar si startDate es null")
    void getMovementsByDateRange_startDateNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByDateRange(null, LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Las fechas son obligatorias");

        verify(kardexRepository, never()).findByMovementDateBetweenOrderByMovementDateDesc(any(), any());
    }

    @Test
    @DisplayName("getMovementsByDateRange: debe fallar si endDate es null")
    void getMovementsByDateRange_endDateNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByDateRange(LocalDate.now(), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Las fechas son obligatorias");

        verify(kardexRepository, never()).findByMovementDateBetweenOrderByMovementDateDesc(any(), any());
    }

    @Test
    @DisplayName("getMovementsByDateRange: debe fallar si startDate es posterior a endDate")
    void getMovementsByDateRange_startDateAfterEndDate_throwsBadRequest() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 12, 31);
        LocalDate endDate = LocalDate.of(2025, 1, 1);

        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByDateRange(startDate, endDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("fecha de inicio debe ser anterior a la fecha de fin");

        verify(kardexRepository, never()).findByMovementDateBetweenOrderByMovementDateDesc(any(), any());
    }

    // ===== TESTS PARA getAllMovements =====

    @Test
    @DisplayName("getAllMovements: debe retornar todos los movimientos")
    void getAllMovements_returnsAllMovements() {
        // Given
        KardexEntity k1 = new KardexEntity();
        k1.setId(1L);
        KardexEntity k2 = new KardexEntity();
        k2.setId(2L);

        when(kardexRepository.findAllByOrderByMovementDateDesc()).thenReturn(List.of(k1, k2));

        // When
        List<KardexEntity> result = kardexService.getAllMovements();

        // Then
        assertThat(result).hasSize(2);
        verify(kardexRepository).findAllByOrderByMovementDateDesc();
    }

    @Test
    @DisplayName("getAllMovements: debe retornar lista vacía si no hay movimientos")
    void getAllMovements_returnsEmptyList() {
        // Given
        when(kardexRepository.findAllByOrderByMovementDateDesc()).thenReturn(List.of());

        // When
        List<KardexEntity> result = kardexService.getAllMovements();

        // Then
        assertThat(result).isEmpty();
        verify(kardexRepository).findAllByOrderByMovementDateDesc();
    }

    // ===== TESTS PARA getMovementsByType =====

    @Test
    @DisplayName("getMovementsByType: debe retornar movimientos del tipo especificado")
    void getMovementsByType_success() {
        // Given
        KardexEntity k1 = new KardexEntity();
        k1.setId(1L);
        k1.setMovementType("PRESTAMO");

        when(kardexRepository.findByMovementTypeOrderByMovementDateDesc("PRESTAMO"))
                .thenReturn(List.of(k1));

        // When
        List<KardexEntity> result = kardexService.getMovementsByType("prestamo");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMovementType()).isEqualTo("PRESTAMO");
        verify(kardexRepository).findByMovementTypeOrderByMovementDateDesc("PRESTAMO");
    }

    @Test
    @DisplayName("getMovementsByType: debe convertir tipo a mayúsculas")
    void getMovementsByType_convertsToUppercase() {
        // Given
        when(kardexRepository.findByMovementTypeOrderByMovementDateDesc("DEVOLUCION"))
                .thenReturn(List.of());

        // When
        kardexService.getMovementsByType("devolucion");

        // Then
        verify(kardexRepository).findByMovementTypeOrderByMovementDateDesc("DEVOLUCION");
    }

    @Test
    @DisplayName("getMovementsByType: debe fallar si movementType es null")
    void getMovementsByType_movementTypeNull_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByType(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tipo de movimiento es obligatorio");

        verify(kardexRepository, never()).findByMovementTypeOrderByMovementDateDesc(any());
    }

    @Test
    @DisplayName("getMovementsByType: debe fallar si movementType es blank")
    void getMovementsByType_movementTypeBlank_throwsBadRequest() {
        // When & Then
        assertThatThrownBy(() -> kardexService.getMovementsByType("   "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("tipo de movimiento es obligatorio");

        verify(kardexRepository, never()).findByMovementTypeOrderByMovementDateDesc(any());
    }
}