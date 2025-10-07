package com.example.demo.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", OffsetDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path
        ));
    }

    // 400 – Bean validation en el body (@Valid) antes de llegar a JPA
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest req) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("Solicitud inválida");
        return body(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }

    // 400 – Bean validation en persistencia (Hibernate dispara jakarta.validation.ConstraintViolationException)
    @ExceptionHandler(ConstraintViolationException.class) // OJO: jakarta.validation.*
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex,
                                                                         HttpServletRequest req) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Solicitud inválida");
        return body(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }

    // 409 – Restricciones de BD (unique, FK, etc.)
    @ExceptionHandler({DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class}) // la de Hibernate
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(Exception ex, HttpServletRequest req) {
        return body(HttpStatus.CONFLICT, "El nombre de herramienta ya existe", req.getRequestURI());
    }

    // 404 – búsquedas que no existen
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    // 400/409 – reglas de negocio (elige el código que uses)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    // 404 – por si usas NoSuchElementException en repos/servicios
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNoSuchElement(java.util.NoSuchElementException ex,
                                                                   HttpServletRequest req) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }
}

