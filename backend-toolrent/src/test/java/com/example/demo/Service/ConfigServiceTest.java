package com.example.demo.Service;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Repository.ConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private ConfigService configService;

    private ConfigEntity config(String key, Double value) {
        ConfigEntity c = new ConfigEntity();
        c.setId(1L);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setDescription("Test config");
        c.setLastModified(LocalDateTime.now());
        c.setModifiedBy("admin");
        return c;
    }

    @Test
    @DisplayName("getAllConfigs: debe retornar todas las configuraciones")
    void getAllConfigs_returnsAll() {
        when(configRepository.findAll()).thenReturn(
                List.of(
                        config("TARIFA_ARRIENDO_DIARIA", 5000.0),
                        config("TARIFA_MULTA_DIARIA", 2000.0)
                )
        );

        List<ConfigEntity> result = configService.getAllConfigs();

        assertThat(result).hasSize(2);
        verify(configRepository).findAll();
    }

    @Test
    @DisplayName("getConfigByKey: debe retornar configuración existente")
    void getConfigByKey_found() {
        String key = "TARIFA_ARRIENDO_DIARIA";
        ConfigEntity expected = config(key, 5000.0);

        when(configRepository.findByConfigKey(key)).thenReturn(Optional.of(expected));

        ConfigEntity result = configService.getConfigByKey(key);

        assertThat(result).isEqualTo(expected);
        assertThat(result.getConfigKey()).isEqualTo(key);
        verify(configRepository).findByConfigKey(key);
    }

    @Test
    @DisplayName("getConfigByKey: debe lanzar excepción si no existe")
    void getConfigByKey_notFound() {
        String key = "NO_EXISTE";
        when(configRepository.findByConfigKey(key)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getConfigByKey(key))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verify(configRepository).findByConfigKey(key);
    }

    @Test
    @DisplayName("getTarifaArriendoDiaria: debe retornar valor de tarifa de arriendo")
    void getTarifaArriendoDiaria_ok() {
        ConfigEntity config = config(ConfigService.TARIFA_ARRIENDO_DIARIA, 5000.0);
        when(configRepository.findByConfigKey(ConfigService.TARIFA_ARRIENDO_DIARIA))
                .thenReturn(Optional.of(config));

        Double result = configService.getTarifaArriendoDiaria();

        assertThat(result).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("getTarifaMultaDiaria: debe retornar valor de tarifa de multa")
    void getTarifaMultaDiaria_ok() {
        ConfigEntity config = config(ConfigService.TARIFA_MULTA_DIARIA, 2000.0);
        when(configRepository.findByConfigKey(ConfigService.TARIFA_MULTA_DIARIA))
                .thenReturn(Optional.of(config));

        Double result = configService.getTarifaMultaDiaria();

        assertThat(result).isEqualTo(2000.0);
    }

    // ==================== ✅ NUEVOS TESTS: CARGO POR REPARACIÓN ====================

    @Nested
    @DisplayName("getCargoReparacion() - Épica 2 RN #16")
    class GetCargoReparacionTests {

        @Test
        @DisplayName("✅ Debe retornar valor de cargo por reparación")
        void getCargoReparacion_ok() {
            // Given
            ConfigEntity config = config(ConfigService.CARGO_REPARACION, 10000.0);
            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.of(config));

            // When
            Double result = configService.getCargoReparacion();

            // Then
            assertThat(result).isEqualTo(10000.0);
            verify(configRepository).findByConfigKey(ConfigService.CARGO_REPARACION);
        }

        @Test
        @DisplayName("✅ Debe lanzar excepción si no existe configuración")
        void getCargoReparacion_notFound() {
            // Given
            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> configService.getCargoReparacion())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("CARGO_REPARACION");
                    });

            verify(configRepository).findByConfigKey(ConfigService.CARGO_REPARACION);
        }
    }

    @Nested
    @DisplayName("setCargoReparacion() - Épica 2 RN #16")
    class SetCargoReparacionTests {

        @Test
        @DisplayName("✅ Debe actualizar cargo por reparación correctamente")
        void setCargoReparacion_ok() {
            // Given
            ConfigEntity existing = config(ConfigService.CARGO_REPARACION, 10000.0);

            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.of(existing));
            when(configRepository.save(any(ConfigEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ConfigEntity result = configService.setCargoReparacion(15000.0, "admin");

            // Then
            assertThat(result.getConfigValue()).isEqualTo(15000.0);
            assertThat(result.getModifiedBy()).isEqualTo("admin");
            assertThat(result.getLastModified()).isNotNull();
            verify(configRepository).save(existing);
        }

        @Test
        @DisplayName("✅ Debe rechazar valor negativo")
        void setCargoReparacion_negativeValue() {
            // When/Then
            assertThatThrownBy(() -> configService.setCargoReparacion(-1000.0, "admin"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains("mayor o igual a 0");
                    });

            verify(configRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Debe rechazar valor null")
        void setCargoReparacion_nullValue() {
            // When/Then
            assertThatThrownBy(() -> configService.setCargoReparacion(null, "admin"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            verify(configRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Debe permitir valor cero (cargo gratuito)")
        void setCargoReparacion_zeroValue() {
            // Given
            ConfigEntity existing = config(ConfigService.CARGO_REPARACION, 10000.0);

            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.of(existing));
            when(configRepository.save(any(ConfigEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            ConfigEntity result = configService.setCargoReparacion(0.0, "admin");

            // Then
            assertThat(result.getConfigValue()).isEqualTo(0.0);
            verify(configRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("Integración de todas las tarifas")
    class IntegrationTests {

        @Test
        @DisplayName("✅ Debe poder cambiar cargo múltiples veces")
        void changeCargoMultipleTimes() {
            // Given
            ConfigEntity config = config(ConfigService.CARGO_REPARACION, 10000.0);

            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.of(config));
            when(configRepository.save(any(ConfigEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            configService.setCargoReparacion(15000.0, "admin1");
            configService.setCargoReparacion(20000.0, "admin2");
            configService.setCargoReparacion(12000.0, "admin3");

            // Then
            verify(configRepository, times(3)).save(config);
            assertThat(config.getConfigValue()).isEqualTo(12000.0);
            assertThat(config.getModifiedBy()).isEqualTo("admin3");
        }

        @Test
        @DisplayName("✅ Todas las tarifas deben poder obtenerse simultáneamente")
        void getAllTarifas_success() {
            // Given
            ConfigEntity arriendo = config(ConfigService.TARIFA_ARRIENDO_DIARIA, 5000.0);
            ConfigEntity multa = config(ConfigService.TARIFA_MULTA_DIARIA, 2000.0);
            ConfigEntity reparacion = config(ConfigService.CARGO_REPARACION, 10000.0);

            when(configRepository.findByConfigKey(ConfigService.TARIFA_ARRIENDO_DIARIA))
                    .thenReturn(Optional.of(arriendo));
            when(configRepository.findByConfigKey(ConfigService.TARIFA_MULTA_DIARIA))
                    .thenReturn(Optional.of(multa));
            when(configRepository.findByConfigKey(ConfigService.CARGO_REPARACION))
                    .thenReturn(Optional.of(reparacion));

            // When
            Double tarifaArriendo = configService.getTarifaArriendoDiaria();
            Double tarifaMulta = configService.getTarifaMultaDiaria();
            Double cargoReparacion = configService.getCargoReparacion();

            // Then
            assertThat(tarifaArriendo).isEqualTo(5000.0);
            assertThat(tarifaMulta).isEqualTo(2000.0);
            assertThat(cargoReparacion).isEqualTo(10000.0);
        }
    }

    // ==================== TESTS ORIGINALES ====================

    @Test
    @DisplayName("updateConfig: debe actualizar valor y datos de modificación")
    void updateConfig_ok() {
        String key = "TARIFA_ARRIENDO_DIARIA";
        ConfigEntity existing = config(key, 5000.0);

        when(configRepository.findByConfigKey(key)).thenReturn(Optional.of(existing));
        when(configRepository.save(any(ConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.updateConfig(key, 6000.0, "admin");

        assertThat(result.getConfigValue()).isEqualTo(6000.0);
        assertThat(result.getModifiedBy()).isEqualTo("admin");
        assertThat(result.getLastModified()).isNotNull();
        verify(configRepository).save(existing);
    }

    @Test
    @DisplayName("updateConfig: debe rechazar valor negativo")
    void updateConfig_negativeValue() {
        String key = "TARIFA_ARRIENDO_DIARIA";

        assertThatThrownBy(() -> configService.updateConfig(key, -100.0, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("mayor o igual a 0");
                });

        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateConfig: debe rechazar valor null")
    void updateConfig_nullValue() {
        String key = "TARIFA_ARRIENDO_DIARIA";

        assertThatThrownBy(() -> configService.updateConfig(key, null, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("setTarifaArriendoDiaria: debe actualizar tarifa de arriendo")
    void setTarifaArriendoDiaria_ok() {
        ConfigEntity config = config(ConfigService.TARIFA_ARRIENDO_DIARIA, 5000.0);

        when(configRepository.findByConfigKey(ConfigService.TARIFA_ARRIENDO_DIARIA))
                .thenReturn(Optional.of(config));
        when(configRepository.save(any(ConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.setTarifaArriendoDiaria(7000.0, "admin");

        assertThat(result.getConfigValue()).isEqualTo(7000.0);
        assertThat(result.getModifiedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("setTarifaMultaDiaria: debe actualizar tarifa de multa")
    void setTarifaMultaDiaria_ok() {
        ConfigEntity config = config(ConfigService.TARIFA_MULTA_DIARIA, 2000.0);

        when(configRepository.findByConfigKey(ConfigService.TARIFA_MULTA_DIARIA))
                .thenReturn(Optional.of(config));
        when(configRepository.save(any(ConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.setTarifaMultaDiaria(3000.0, "admin");

        assertThat(result.getConfigValue()).isEqualTo(3000.0);
        assertThat(result.getModifiedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("getConfigById: debe retornar configuración por ID")
    void getConfigById_found() {
        Long id = 1L;
        ConfigEntity expected = config("TARIFA_ARRIENDO_DIARIA", 5000.0);

        when(configRepository.findById(id)).thenReturn(Optional.of(expected));

        ConfigEntity result = configService.getConfigById(id);

        assertThat(result).isEqualTo(expected);
        verify(configRepository).findById(id);
    }

    @Test
    @DisplayName("getConfigById: debe lanzar excepción si no existe")
    void getConfigById_notFound() {
        Long id = 999L;
        when(configRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getConfigById(id))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("updateConfigById: debe actualizar configuración por ID")
    void updateConfigById_ok() {
        Long id = 1L;
        ConfigEntity existing = config("TARIFA_ARRIENDO_DIARIA", 5000.0);

        when(configRepository.findById(id)).thenReturn(Optional.of(existing));
        when(configRepository.save(any(ConfigEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfigEntity result = configService.updateConfigById(id, 8000.0, "admin");

        assertThat(result.getConfigValue()).isEqualTo(8000.0);
        assertThat(result.getModifiedBy()).isEqualTo("admin");
        verify(configRepository).save(existing);
    }
}