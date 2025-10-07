package com.example.demo.Service;

import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ToolRepository;
import com.example.demo.Service.ToolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock private ToolRepository toolRepository;
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
    }

    @Test
    @DisplayName("create falla si nombre es vacío o blank")
    void create_blankName() {
        ToolEntity body = tool("   ", null, 1);

        assertThatThrownBy(() -> toolService.create(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre es obligatorio");

        verify(toolRepository, never()).save(any());
    }

    @Test
    @DisplayName("create falla si nombre ya existe (único por ignoreCase)")
    void create_duplicateName() {
        ToolEntity body = tool("Taladro", null, 1);
        when(toolRepository.existsByNameIgnoreCase("Taladro")).thenReturn(true);

        assertThatThrownBy(() -> toolService.create(body))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(toolRepository, never()).save(any());
    }
}

