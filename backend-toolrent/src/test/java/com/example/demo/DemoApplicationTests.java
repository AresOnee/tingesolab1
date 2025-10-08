package com.example.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class DemoApplicationTests {

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        // Este test verifica que el contexto de Spring carga correctamente
        // Si llega aquí, significa que la aplicación arrancó sin errores
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("Main method executes without errors")
    void mainMethodExecutes() {
        // Verifica que el metodo main puede ejecutarse
        // Esto aumentará la cobertura del metodo main
        assertThatCode(() -> {
            DemoApplication.main(new String[] {});
        }).doesNotThrowAnyException();
    }
}