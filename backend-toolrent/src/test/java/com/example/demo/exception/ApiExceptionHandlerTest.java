package com.example.demo.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @InjectMocks
    private ApiExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("handleBeanValidation: debe retornar 400 con mensaje de validación")
    void handleBeanValidation_shouldReturn400() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "El campo es obligatorio");
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleBeanValidation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("El campo es obligatorio");
        assertThat(response.getBody().get("path")).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("handleBeanValidation: debe manejar errores sin mensajes específicos")
    void handleBeanValidation_noSpecificMessage() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(java.util.List.of());

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleBeanValidation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("Solicitud inválida");
    }

    @Test
    @DisplayName("handleConstraintViolation: debe retornar 400 con mensaje de constraint")
    void handleConstraintViolation_shouldReturn400() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("El valor debe ser positivo");
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("El valor debe ser positivo");
    }

    @Test
    @DisplayName("handleConstraintViolation: debe manejar violations vacías")
    void handleConstraintViolation_emptyViolations() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("Solicitud inválida");
    }

    @Test
    @DisplayName("handleDataIntegrity: debe retornar 409 por DataIntegrityViolationException")
    void handleDataIntegrity_DataIntegrityViolationException() {
        // Given
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat(response.getBody().get("message")).isEqualTo("El nombre de herramienta ya existe");
    }

    @Test
    @DisplayName("handleDataIntegrity: debe retornar 409 por Hibernate ConstraintViolationException")
    void handleDataIntegrity_HibernateConstraintViolation() {
        // Given
        org.hibernate.exception.ConstraintViolationException ex =
                new org.hibernate.exception.ConstraintViolationException("Constraint violation", null, "uq_name");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrity(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("message")).isEqualTo("El nombre de herramienta ya existe");
    }

    @Test
    @DisplayName("handleNotFound: debe retornar 404 con mensaje personalizado")
    void handleNotFound_shouldReturn404() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Cliente no encontrado");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).isEqualTo("Cliente no encontrado");
        assertThat(response.getBody().get("error")).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("handleIllegalArgument: debe retornar 400 por IllegalArgumentException")
    void handleIllegalArgument_shouldReturn400() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Parámetro inválido");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message")).isEqualTo("Parámetro inválido");
    }

    @Test
    @DisplayName("handleNoSuchElement: debe retornar 404 por NoSuchElementException")
    void handleNoSuchElement_shouldReturn404() {
        // Given
        java.util.NoSuchElementException ex = new java.util.NoSuchElementException("Elemento no encontrado");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleNoSuchElement(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).isEqualTo("Elemento no encontrado");
    }

    @Test
    @DisplayName("Todas las respuestas deben incluir timestamp")
    void allResponses_shouldIncludeTimestamp() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Test");

        // When
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex, request);

        // Then
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
    }
}