package com.example.demo.Controller;

import com.example.demo.Entity.ConfigEntity;
import com.example.demo.Service.ConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConfigController.class)

@AutoConfigureMockMvc
class ConfigControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConfigService configService;


    @Test
    @DisplayName("GET /api/v1/config => 200 con lista de configuraciones")
    @WithMockUser(roles = "USER")
    void getAllConfigs_ok() throws Exception {
        ConfigEntity config1 = config(1L, "TARIFA_ARRIENDO_DIARIA", 5000.0);
        ConfigEntity config2 = config(2L, "TARIFA_MULTA_DIARIA", 2000.0);
        when(configService.getAllConfigs()).thenReturn(List.of(config1, config2));
        mvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
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
                .andExpect(jsonPath("$.configKey").value(key));
    }

    @Test
    @DisplayName("GET /api/v1/config/key/{configKey} => 404 si no existe")
    @WithMockUser(roles = "USER")
    void getConfigByKey_notFound() throws Exception {
        String key = "CLAVE_INEXISTENTE";
        when(configService.getConfigByKey(key))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuracion no encontrada"));
        mvc.perform(get("/api/v1/config/key/{configKey}", key))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/config/tarifa-arriendo => 200 con valor")
    @WithMockUser(roles = "USER")
    void getTarifaArriendo_ok() throws Exception {
        when(configService.getTarifaArriendoDiaria()).thenReturn(5000.0);
        mvc.perform(get("/api/v1/config/tarifa-arriendo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaArriendoDiaria").value(5000.0));
    }

    @Test
    @DisplayName("GET /api/v1/config/tarifa-multa => 200 con valor")
    @WithMockUser(roles = "USER")
    void getTarifaMulta_ok() throws Exception {
        when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
        mvc.perform(get("/api/v1/config/tarifa-multa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaMultaDiaria").value(2000.0));
    }

    @Test
    @DisplayName("PUT /api/v1/config/{id} => 200 con configuracion actualizada")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateConfig_ok() throws Exception {
        Long id = 1L;
        ConfigEntity updated = config(id, "TARIFA_ARRIENDO_DIARIA", 6000.0);
        when(configService.updateConfigById(eq(id), eq(6000.0), eq("admin"))).thenReturn(updated);
        mvc.perform(put("/api/v1/config/{id}", id)
                        .with(csrf()) // CAMBIO 3: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 6000.0}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/config/{id} => 400 si valor negativo")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateConfig_badRequest() throws Exception {
        Long id = 1L;
        when(configService.updateConfigById(eq(id), eq(-100.0), eq("admin")))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        mvc.perform(put("/api/v1/config/{id}", id)
                        .with(csrf()) // CAMBIO 3: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": -100.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/config/tarifa-arriendo => 200 con tarifa actualizada")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateTarifaArriendo_ok() throws Exception {
        ConfigEntity updated = config(1L, "TARIFA_ARRIENDO_DIARIA", 7000.0);
        when(configService.setTarifaArriendoDiaria(eq(7000.0), eq("admin"))).thenReturn(updated);
        mvc.perform(put("/api/v1/config/tarifa-arriendo")
                        .with(csrf()) // CAMBIO 3: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 7000.0}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/config/tarifa-multa => 200 con tarifa actualizada")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateTarifaMulta_ok() throws Exception {
        ConfigEntity updated = config(2L, "TARIFA_MULTA_DIARIA", 3000.0);
        when(configService.setTarifaMultaDiaria(eq(3000.0), eq("admin"))).thenReturn(updated);
        mvc.perform(put("/api/v1/config/tarifa-multa")
                        .with(csrf()) // CAMBIO 3: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 3000.0}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/config/{id} => 404 si no existe")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateConfig_notFound() throws Exception {
        Long id = 999L;
        when(configService.updateConfigById(eq(id), eq(5000.0), eq("admin")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(put("/api/v1/config/{id}", id)
                        .with(csrf()) // CAMBIO 3: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": 5000.0}"))
                .andExpect(status().isNotFound());
    }

    private ConfigEntity config(Long id, String key, Double value) {
        ConfigEntity config = new ConfigEntity();
        config.setId(id);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription("Descripcion de " + key);
        config.setLastModified(LocalDateTime.now());
        config.setModifiedBy("test-user");
        return config;
    }
}