package com.example.demo.Service;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock private ToolRepository toolRepository;
    @Mock private KardexService kardexService; // ✅ AGREGAR MOCK
    @InjectMocks private ToolService toolService;

    private ToolEntity tool(String name, String status, Integer stock) {
        ToolEntity t = new ToolEntity();
        t.setName(name);
        t.setCategory("Cat");
        t.setReplacementValue(1000);
        t.setStock(stock);
        t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("getAllTools delega en repository y retorna lista")
    void getAllTools_returnsList() {
        when(toolRepository.findAll()).thenReturn(List.of(tool("Taladro", "Disponible", 2)));

        List<ToolEntity> result = toolService.getAllTools();

        assertThat(result).hasSize(1);
        verify(toolRepository).findAll();
    }

    @Test
    @DisplayName("saveTool setea 'Disponible' si status es null")
    void saveTool_defaultsStatus() {
        ToolEntity t = tool("Taladro", null, 1);
        when(toolRepository.save(any(ToolEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0, ToolEntity.class));

        ToolEntity saved = toolService.saveTool(t);

        assertThat(saved.getStatus()).isEqualTo("Disponible");
        verify(toolRepository).save(any(ToolEntity.class));
    }

    @Test
    @DisplayName("create OK: nombre válido y único; status por defecto")
    void create_ok() {
        ToolEntity body = tool("Taladro", null, 3);

        when(toolRepository.existsByNameIgnoreCase("Taladro")).thenReturn(false);
        when(toolRepository.save(any(ToolEntity.class)))
                .thenAnswer(inv -> {
                    ToolEntity s = inv.getArgument(0, ToolEntity.class);
                    s.setId(10L);
                    return s;
                });

        ToolEntity saved = toolService.create(body);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Taladro");
        assertThat(saved.getStatus()).isEqualTo("Disponible");
        verify(toolRepository).save(any(ToolEntity.class));
        verify(kardexService).registerMovement(any(), eq("REGISTRO"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create falla si nombre es vacío o blank")
    void create_blankName() {
        ToolEntity body = tool("   ", null, 1);

        assertThatThrownBy(() -> toolService.create(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre es obligatorio");

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create falla si nombre ya existe (único por ignoreCase)")
    void create_duplicateName() {
        ToolEntity body = tool("Taladro", null, 1);
        when(toolRepository.existsByNameIgnoreCase("Taladro")).thenReturn(true);

        assertThatThrownBy(() -> toolService.create(body))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    // ========== NUEVOS TESTS PARA CUBRIR BRANCHES FALTANTES ==========

    @Test
    @DisplayName("saveTool: debe mantener status cuando no es null ni blank")
    void saveTool_keepsStatusWhenNotNullOrBlank() {
        // Given
        ToolEntity tool = tool("Taladro", "En reparación", 2);
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ToolEntity saved = toolService.saveTool(tool);

        // Then
        assertThat(saved.getStatus()).isEqualTo("En reparación");
        verify(toolRepository).save(tool);
    }

    @Test
    @DisplayName("saveTool: debe setear 'Disponible' cuando status es blank")
    void saveTool_setsDisponibleWhenStatusIsBlank() {
        // Given
        ToolEntity tool = tool("Taladro", "   ", 2); // blank
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ToolEntity saved = toolService.saveTool(tool);

        // Then
        assertThat(saved.getStatus()).isEqualTo("Disponible");
        verify(toolRepository).save(tool);
    }

    @Test
    @DisplayName("create: debe setear 'Disponible' cuando status es blank (no solo null)")
    void create_setsDisponibleWhenStatusIsBlank() {
        // Given
        ToolEntity tool = tool("Sierra", "  ", 3);
        when(toolRepository.existsByNameIgnoreCase("Sierra")).thenReturn(false);
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        // When
        ToolEntity saved = toolService.create(tool);

        // Then
        assertThat(saved.getStatus()).isEqualTo("Disponible");
        verify(toolRepository).save(tool);
        verify(kardexService).registerMovement(any(), eq("REGISTRO"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create: debe mantener status cuando tiene valor válido")
    void create_keepsValidStatus() {
        // Given
        ToolEntity tool = tool("Martillo", "Prestada", 1);
        when(toolRepository.existsByNameIgnoreCase("Martillo")).thenReturn(false);
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        // When
        ToolEntity saved = toolService.create(tool);

        // Then
        assertThat(saved.getStatus()).isEqualTo("Prestada");
        verify(toolRepository).save(tool);
        verify(kardexService).registerMovement(any(), eq("REGISTRO"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create: debe trimear el nombre antes de validar")
    void create_trimsNameBeforeValidation() {
        // Given
        ToolEntity tool = tool("  Taladro  ", "Disponible", 2);
        when(toolRepository.existsByNameIgnoreCase("Taladro")).thenReturn(false);
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        // When
        ToolEntity saved = toolService.create(tool);

        // Then
        verify(toolRepository).existsByNameIgnoreCase("Taladro");
        verify(toolRepository).save(any(ToolEntity.class));
        verify(kardexService).registerMovement(any(), eq("REGISTRO"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create: debe validar nombre null como blank")
    void create_nullNameIsBlank() {
        // Given
        ToolEntity tool = tool(null, "Disponible", 2);

        // When & Then
        assertThatThrownBy(() -> toolService.create(tool))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El nombre es obligatorio");

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create: debe lanzar excepción cuando nombre existe (case insensitive)")
    void create_duplicateNameCaseInsensitive() {
        // Given
        ToolEntity tool = tool("TALADRO", "Disponible", 1);
        when(toolRepository.existsByNameIgnoreCase("TALADRO")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> toolService.create(tool))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_tools_name");

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("create: debe permitir crear cuando nombre es único (case insensitive)")
    void create_allowsUniqueNameCaseInsensitive() {
        // Given
        ToolEntity tool = tool("Sierra Circular", "Disponible", 3);
        when(toolRepository.existsByNameIgnoreCase("Sierra Circular")).thenReturn(false);
        when(toolRepository.save(any(ToolEntity.class))).thenAnswer(inv -> {
            ToolEntity t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        // When
        ToolEntity saved = toolService.create(tool);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Sierra Circular");
        verify(toolRepository).existsByNameIgnoreCase("Sierra Circular");
        verify(toolRepository).save(any(ToolEntity.class));
        verify(kardexService).registerMovement(any(), eq("REGISTRO"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("getAllTools: debe retornar lista vacía cuando no hay herramientas")
    void getAllTools_emptyList() {
        // Given
        when(toolRepository.findAll()).thenReturn(List.of());

        // When
        List<ToolEntity> result = toolService.getAllTools();

        // Then
        assertThat(result).isEmpty();
        verify(toolRepository).findAll();
    }

    @Test
    @DisplayName("getAllTools: debe retornar todas las herramientas")
    void getAllTools_returnsAllTools() {
        // Given
        ToolEntity t1 = tool("Taladro", "Disponible", 2);
        ToolEntity t2 = tool("Martillo", "Prestada", 0);
        when(toolRepository.findAll()).thenReturn(List.of(t1, t2));

        // When
        List<ToolEntity> result = toolService.getAllTools();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(t1, t2);
        verify(toolRepository).findAll();
    }

    // ===== TESTS PARA RF1.2: DAR DE BAJA HERRAMIENTAS =====

    @Test
    @DisplayName("decommission OK: cambia estado a 'Dada de baja' y stock a 0")
    void decommission_ok() {
        Long toolId = 1L;
        ToolEntity tool = tool("Taladro", "Disponible", 5);
        tool.setId(toolId);

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(ToolEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ToolEntity result = toolService.decommission(toolId);

        assertThat(result.getStatus()).isEqualTo("Dada de baja");
        assertThat(result.getStock()).isEqualTo(0);
        verify(toolRepository).save(tool);
        verify(kardexService).registerMovement(any(), eq("BAJA"), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("decommission falla si herramienta no existe")
    void decommission_notFound() {
        Long toolId = 999L;
        when(toolRepository.findById(toolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolService.decommission(toolId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Herramienta no encontrada");
                });

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("decommission falla si herramienta está prestada")
    void decommission_toolIsLoaned() {
        Long toolId = 1L;
        ToolEntity tool = tool("Taladro", "Prestada", 3);
        tool.setId(toolId);

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.decommission(toolId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("No se puede dar de baja una herramienta que está prestada");
                });

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("decommission falla si herramienta ya está dada de baja")
    void decommission_alreadyDecommissioned() {
        Long toolId = 1L;
        ToolEntity tool = tool("Taladro", "Dada de baja", 0);
        tool.setId(toolId);

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));

        assertThatThrownBy(() -> toolService.decommission(toolId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("La herramienta ya está dada de baja");
                });

        verify(toolRepository, never()).save(any());
        verify(kardexService, never()).registerMovement(any(), any(), any(), any(), any(), any()); // ✅
    }

    @Test
    @DisplayName("decommission OK desde estado 'En reparación'")
    void decommission_fromRepair() {
        Long toolId = 1L;
        ToolEntity tool = tool("Taladro", "En reparación", 2);
        tool.setId(toolId);

        when(toolRepository.findById(toolId)).thenReturn(Optional.of(tool));
        when(toolRepository.save(any(ToolEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ToolEntity result = toolService.decommission(toolId);

        assertThat(result.getStatus()).isEqualTo("Dada de baja");
        assertThat(result.getStock()).isEqualTo(0);
        verify(toolRepository).save(tool);
        verify(kardexService).registerMovement(any(), eq("BAJA"), any(), any(), any(), any()); // ✅
    }
}