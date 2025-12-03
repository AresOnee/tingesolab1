package com.example.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        // Configuramos puerto 0 para que use un puerto aleatorio
        assertThatCode(() -> {
            DemoApplication.main(new String[] {"--server.port=0"});
        }).doesNotThrowAnyException();
    }
}