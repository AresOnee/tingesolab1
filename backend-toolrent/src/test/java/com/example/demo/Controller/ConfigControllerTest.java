package com.example.demo.Controller;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Service.ConfigService;
import com.example.demo.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConfigController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ConfigControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    @SuppressWarnings("removal")
    private ConfigService configService;

    // Helper para crear ConfigEntity
    private ConfigEntity config(Long id, String key, Double value) {
        ConfigEntity c = new ConfigEntity();
        c.setId(id);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setDescription("Test config");
        c.setLastModified(LocalDateTime.now());
        c.setModifiedBy("testuser");
        return c;
    }

    // ===========================================================
    // Tests GET (no requieren autenticación especial)
    // ===========================================================

    @Test
    @DisplayName("GET /api/v1/config => 200 con lista de configuraciones")
    @WithMockUser(roles = "USER")
    void getAllConfigs_ok() throws Exception {
        ConfigEntity config1 = config(1L, "TARIFA_ARRIENDO_DIARIA", 5000.0);
        ConfigEntity config2 = config(2L, "TARIFA_MULTA_DIARIA", 2000.0);
        when(configService.getAllConfigs()).thenReturn(List.of(config1, config2));

        mvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].configKey").value("TARIFA_ARRIENDO_DIARIA"))
                .andExpect(jsonPath("$[1].configKey").value("TARIFA_MULTA_DIARIA"));
    }

    @Test
    @DisplayName("GET /api/v1/config/key/{configKey} => 200 con configuracion encontrada")
    @WithMockUser(roles = "USER")
    void getConfigByKey_ok() throws Exception {
        String key = "TARIFA_ARRIENDO_DIARIA";
        ConfigEntity config = config(1L, key, 5000.0);
        when(configService.getConfigByKey(key)).thenReturn(config);

        mvc.perform(get("/api/v1/config/key/{configKey}", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configKey").value(key))
                .andExpect(jsonPath("$.configValue").value(5000.0));
    }

    @Test
    @DisplayName("GET /api/v1/config/key/{configKey} => 404 si no existe")
    @WithMockUser(roles = "USER")
    void getConfigByKey_notFound() throws Exception {
        String key = "CLAVE_INEXISTENTE";
        when(configService.getConfigByKey(key))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Config no encontrada"));

        mvc.perform(get("/api/v1/config/key/{configKey}", key))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/config/tarifa-arriendo => 200 con tarifa")
    @WithMockUser(roles = "USER")
    void getTarifaArriendo_ok() throws Exception {
        when(configService.getTarifaArriendoDiaria()).thenReturn(7000.0);

        mvc.perform(get("/api/v1/config/tarifa-arriendo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaArriendoDiaria").value(7000.0));
    }

    @Test
    @DisplayName("GET /api/v1/config/tarifa-multa => 200 con tarifa")
    @WithMockUser(roles = "USER")
    void getTarifaMulta_ok() throws Exception {
        when(configService.getTarifaMultaDiaria()).thenReturn(5000.0);

        mvc.perform(get("/api/v1/config/tarifa-multa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaMultaDiaria").value(5000.0));
    }

    // ===========================================================
    // ✅ TESTS JWT: Con authorities directas
    // ===========================================================

    @Nested
    @DisplayName("Tests de extracción de username desde JWT")
    class UsernameExtractionTests {

        @Test
        @DisplayName("PUT /api/v1/config/{id} con JWT válido (preferred_username)")
        void updateConfig_withPreferredUsername() throws Exception {
            // Given
            Long configId = 1L;
            Double newValue = 8000.0;
            String expectedUsername = "diego";

            ConfigEntity updated = config(configId, "TARIFA_ARRIENDO_DIARIA", newValue);
            updated.setModifiedBy(expectedUsername);

            when(configService.updateConfigById(eq(configId), eq(newValue), eq(expectedUsername)))
                    .thenReturn(updated);

            // When & Then
            mvc.perform(put("/api/v1/config/{id}", configId)
                            .with(csrf())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))  // ✅ Authorities directas
                                    .jwt(jwt -> jwt
                                            .claim("preferred_username", expectedUsername)
                                            .claim("sub", "882f6bb1-fa29-4e98-bbb8-a078f8b8c4aab")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\": 8000.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modifiedBy").value(expectedUsername));

            verify(configService).updateConfigById(configId, newValue, expectedUsername);
        }

        @Test
        @DisplayName("PUT /api/v1/config/{id} sin preferred_username usa 'name'")
        void updateConfig_withNameFallback() throws Exception {
            // Given
            Long configId = 1L;
            Double newValue = 8000.0;
            String expectedUsername = "Diego Morales";

            ConfigEntity updated = config(configId, "TARIFA_ARRIENDO_DIARIA", newValue);
            updated.setModifiedBy(expectedUsername);

            when(configService.updateConfigById(eq(configId), eq(newValue), eq(expectedUsername)))
                    .thenReturn(updated);

            // When & Then
            mvc.perform(put("/api/v1/config/{id}", configId)
                            .with(csrf())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))  // ✅ Authorities directas
                                    .jwt(jwt -> jwt
                                            .claim("name", expectedUsername)
                                            .claim("sub", "882f6bb1-fa29-4e98-bbb8-a078f8b8c4aab")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\": 8000.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.modifiedBy").value(expectedUsername));

            verify(configService).updateConfigById(configId, newValue, expectedUsername);
        }

        @Test
        @DisplayName("PUT /api/v1/config/tarifa-arriendo con JWT válido")
        void updateTarifaArriendo_extractsUsername() throws Exception {
            // Given
            Double newValue = 10000.0;
            String expectedUsername = "admin";

            ConfigEntity updated = config(1L, "TARIFA_ARRIENDO_DIARIA", newValue);
            updated.setModifiedBy(expectedUsername);

            when(configService.setTarifaArriendoDiaria(eq(newValue), eq(expectedUsername)))
                    .thenReturn(updated);

            // When & Then
            mvc.perform(put("/api/v1/config/tarifa-arriendo")
                            .with(csrf())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))  // ✅ Authorities directas
                                    .jwt(jwt -> jwt.claim("preferred_username", expectedUsername)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\": 10000.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.configValue").value(newValue))
                    .andExpect(jsonPath("$.modifiedBy").value(expectedUsername));

            verify(configService).setTarifaArriendoDiaria(newValue, expectedUsername);
        }

        @Test
        @DisplayName("PUT /api/v1/config/tarifa-multa con JWT válido")
        void updateTarifaMulta_extractsUsername() throws Exception {
            // Given
            Double newValue = 6000.0;
            String expectedUsername = "diego";

            ConfigEntity updated = config(2L, "TARIFA_MULTA_DIARIA", newValue);
            updated.setModifiedBy(expectedUsername);

            when(configService.setTarifaMultaDiaria(eq(newValue), eq(expectedUsername)))
                    .thenReturn(updated);

            // When & Then
            mvc.perform(put("/api/v1/config/tarifa-multa")
                            .with(csrf())
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))  // ✅ Authorities directas
                                    .jwt(jwt -> jwt.claim("preferred_username", expectedUsername)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\": 6000.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.configValue").value(newValue))
                    .andExpect(jsonPath("$.modifiedBy").value(expectedUsername));

            verify(configService).setTarifaMultaDiaria(newValue, expectedUsername);
        }
    }

    // ===========================================================
    // Tests originales mantenidos
    // ===========================================================

    @Test
    @DisplayName("PUT /api/v1/config/{id} => 200 cuando actualiza correctamente")
    @WithMockUser(roles = "ADMIN")
    void updateConfig_ok() throws Exception {
        Long configId = 1L;
        Double newValue = 8000.0;
        ConfigEntity updated = config(configId, "TARIFA_ARRIENDO_DIARIA", newValue);

        when(configService.updateConfigById(eq(configId), eq(newValue), anyString()))
                .thenReturn(updated);

        mvc.perform(put("/api/v1/config/{id}", configId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 8000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(configId))
                .andExpect(jsonPath("$.configValue").value(newValue));
    }

    @Test
    @DisplayName("PUT /api/v1/config/{id} => 404 si configuración no existe")
    @WithMockUser(roles = "ADMIN")
    void updateConfig_notFound() throws Exception {
        Long configId = 999L;
        when(configService.updateConfigById(eq(configId), anyDouble(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Config no encontrada"));

        mvc.perform(put("/api/v1/config/{id}", configId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 8000.0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/config/tarifa-arriendo => 200")
    @WithMockUser(roles = "ADMIN")
    void updateTarifaArriendo_ok() throws Exception {
        Double newValue = 10000.0;
        ConfigEntity updated = config(1L, "TARIFA_ARRIENDO_DIARIA", newValue);

        when(configService.setTarifaArriendoDiaria(eq(newValue), anyString()))
                .thenReturn(updated);

        mvc.perform(put("/api/v1/config/tarifa-arriendo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 10000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configValue").value(newValue));
    }

    @Test
    @DisplayName("PUT /api/v1/config/tarifa-multa => 200")
    @WithMockUser(roles = "ADMIN")
    void updateTarifaMulta_ok() throws Exception {
        Double newValue = 6000.0;
        ConfigEntity updated = config(2L, "TARIFA_MULTA_DIARIA", newValue);

        when(configService.setTarifaMultaDiaria(eq(newValue), anyString()))
                .thenReturn(updated);

        mvc.perform(put("/api/v1/config/tarifa-multa")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 6000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configValue").value(newValue));
    }

    @Test
    @DisplayName("PUT sin rol ADMIN => 403 Forbidden")
    @WithMockUser(username = "user", roles = {"USER"})
    void updateConfig_forbidden_whenNotAdmin() throws Exception {
        mvc.perform(put("/api/v1/config/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 8000.0}"))
                .andExpect(status().isForbidden());
    }
}