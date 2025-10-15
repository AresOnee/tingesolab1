package com.example.demo.Entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests completos para ClientEntity
 *
 * Cubre todas las validaciones implementadas:
 * - @NotBlank en todos los campos obligatorios
 * - @Size en name
 * - @Pattern en rut, phone, state
 * - @Email en email
 * - Constructores
 */
@DisplayName("ClientEntity - Tests de Validaciones")
class ClientEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper para crear un cliente válido por defecto
     */
    private ClientEntity validClient() {
        return new ClientEntity(
                1L,
                "Juan Pérez",
                "12.345.678-9",
                "+56912345678",
                "juan@toolrent.cl",
                "Activo"
        );
    }

    /**
     * Helper para validar y obtener el primer mensaje de error
     */
    private String getFirstViolationMessage(ClientEntity client) {
        Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.iterator().next().getMessage();
    }

    /**
     * Helper para contar número de violaciones
     */
    private int countViolations(ClientEntity client) {
        return validator.validate(client).size();
    }

    // ==================== TESTS DE CONSTRUCTORES ====================

    @Nested
    @DisplayName("Constructores")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor con todos los parámetros debe asignar valores correctamente")
        void constructor_allArgs_assignsValues() {
            // When
            ClientEntity client = new ClientEntity(
                    1L,
                    "Juan Pérez",
                    "12.345.678-9",
                    "+56912345678",
                    "juan@toolrent.cl",
                    "Activo"
            );

            // Then
            assertThat(client.getId()).isEqualTo(1L);
            assertThat(client.getName()).isEqualTo("Juan Pérez");
            assertThat(client.getRut()).isEqualTo("12.345.678-9");
            assertThat(client.getPhone()).isEqualTo("+56912345678");
            assertThat(client.getEmail()).isEqualTo("juan@toolrent.cl");
            assertThat(client.getState()).isEqualTo("Activo");
        }

        @Test
        @DisplayName("Constructor sin state debe asignar 'Activo' por defecto")
        void constructor_withoutState_assignsActivoByDefault() {
            // When
            ClientEntity client = new ClientEntity(
                    1L,
                    "María González",
                    "20.123.456-7",
                    "+56987654321",
                    "maria@toolrent.cl"
            );

            // Then
            assertThat(client.getState()).isEqualTo("Activo");
            assertThat(client.getName()).isEqualTo("María González");
        }

        @Test
        @DisplayName("Constructor sin argumentos debe crear cliente vacío")
        void constructor_noArgs_createsEmptyClient() {
            // When
            ClientEntity client = new ClientEntity();

            // Then
            assertThat(client.getId()).isNull();
            assertThat(client.getName()).isNull();
            assertThat(client.getRut()).isNull();
            assertThat(client.getPhone()).isNull();
            assertThat(client.getEmail()).isNull();
            assertThat(client.getState()).isNull();
        }
    }

    // ==================== TESTS DE VALIDACIÓN: NAME ====================

    @Nested
    @DisplayName("Validación de Name")
    class NameValidationTests {

        @Test
        @DisplayName("Name válido debe pasar validación")
        void name_valid_passesValidation() {
            ClientEntity client = validClient();
            client.setName("Juan Pérez González");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Name null debe fallar con mensaje 'obligatorio'")
        void name_null_failsValidation() {
            ClientEntity client = validClient();
            client.setName(null);

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Name vacío debe fallar con mensaje 'obligatorio'")
        void name_empty_failsValidation() {
            ClientEntity client = validClient();
            client.setName("");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Name solo con espacios debe fallar con mensaje 'obligatorio'")
        void name_blank_failsValidation() {
            ClientEntity client = validClient();
            client.setName("   ");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Name con 1 carácter debe fallar con mensaje de tamaño")
        void name_tooShort_failsValidation() {
            ClientEntity client = validClient();
            client.setName("A");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .contains("entre 2 y 100 caracteres");
        }

        @Test
        @DisplayName("Name con 2 caracteres debe pasar validación")
        void name_minimumLength_passesValidation() {
            ClientEntity client = validClient();
            client.setName("Ab");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Name con más de 100 caracteres debe fallar")
        void name_tooLong_failsValidation() {
            ClientEntity client = validClient();
            client.setName("A".repeat(101));

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .contains("entre 2 y 100 caracteres");
        }
    }

    // ==================== TESTS DE VALIDACIÓN: RUT ====================

    @Nested
    @DisplayName("Validación de RUT")
    class RutValidationTests {

        @Test
        @DisplayName("RUT válido con formato XX.XXX.XXX-X debe pasar validación")
        void rut_validFormat_passesValidation() {
            ClientEntity client = validClient();

            // Probar diferentes RUTs válidos
            String[] validRuts = {
                    "12.345.678-9",
                    "20.204.010-5",
                    "1.234.567-K",
                    "9.876.543-2"
            };

            for (String rut : validRuts) {
                client.setRut(rut);
                Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);
                assertThat(violations)
                        .as("RUT %s debería ser válido", rut)
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("RUT null debe fallar con mensaje 'obligatorio'")
        void rut_null_failsValidation() {
            ClientEntity client = validClient();
            client.setRut(null);

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("RUT vacío debe fallar")
        void rut_empty_failsValidation() {
            ClientEntity client = validClient();
            client.setRut("");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("RUT sin puntos ni guión debe fallar")
        void rut_withoutFormatting_failsValidation() {
            ClientEntity client = validClient();
            client.setRut("123456789");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de RUT inválido");
        }

        @Test
        @DisplayName("RUT sin puntos debe fallar")
        void rut_withoutDots_failsValidation() {
            ClientEntity client = validClient();
            client.setRut("12345678-9");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de RUT inválido");
        }

        @Test
        @DisplayName("RUT sin guión debe fallar")
        void rut_withoutHyphen_failsValidation() {
            ClientEntity client = validClient();
            client.setRut("12.345.6789");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de RUT inválido");
        }

        @Test
        @DisplayName("RUT con más de 2 dígitos iniciales debe fallar")
        void rut_tooManyInitialDigits_failsValidation() {
            ClientEntity client = validClient();
            client.setRut("123.456.789-0");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de RUT inválido");
        }

        @Test
        @DisplayName("RUT con letra en dígito verificador K (mayúscula) debe pasar")
        void rut_withUppercaseK_passesValidation() {
            ClientEntity client = validClient();
            client.setRut("1.234.567-K");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("RUT con letra en dígito verificador k (minúscula) debe pasar")
        void rut_withLowercaseK_passesValidation() {
            ClientEntity client = validClient();
            client.setRut("1.234.567-k");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }
    }

    // ==================== TESTS DE VALIDACIÓN: PHONE ====================

    @Nested
    @DisplayName("Validación de Phone")
    class PhoneValidationTests {

        @Test
        @DisplayName("Phone válido con formato +56XXXXXXXXX debe pasar validación")
        void phone_validFormat_passesValidation() {
            ClientEntity client = validClient();

            // Probar diferentes teléfonos válidos
            String[] validPhones = {
                    "+56912345678",
                    "+56987654321",
                    "+56900000000",
                    "+56999999999"
            };

            for (String phone : validPhones) {
                client.setPhone(phone);
                Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);
                assertThat(violations)
                        .as("Phone %s debería ser válido", phone)
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("Phone null debe fallar con mensaje 'obligatorio'")
        void phone_null_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone(null);

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Phone vacío debe fallar con mensaje 'obligatorio'")
        void phone_empty_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("");

            String message = getFirstViolationMessage(client);

            // Cuando está vacío, @NotBlank falla primero con "obligatorio"
            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Phone con solo espacios debe fallar con mensaje 'obligatorio'")
        void phone_blank_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("   ");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Phone sin +56 debe fallar")
        void phone_withoutPrefix_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("912345678");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de teléfono inválido");
        }

        @Test
        @DisplayName("Phone con menos de 9 dígitos debe fallar")
        void phone_tooShort_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("+5691234567"); // Solo 8 dígitos

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de teléfono inválido");
        }

        @Test
        @DisplayName("Phone con más de 9 dígitos debe fallar")
        void phone_tooLong_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("+569123456789"); // 10 dígitos

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de teléfono inválido");
        }

        @Test
        @DisplayName("Phone con espacios debe fallar")
        void phone_withSpaces_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("+56 9 1234 5678");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de teléfono inválido");
        }

        @Test
        @DisplayName("Phone con código de país incorrecto debe fallar")
        void phone_wrongCountryCode_failsValidation() {
            ClientEntity client = validClient();
            client.setPhone("+54912345678"); // Argentina +54

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("Formato de teléfono inválido");
        }
    }

    // ==================== TESTS DE VALIDACIÓN: EMAIL ====================

    @Nested
    @DisplayName("Validación de Email")
    class EmailValidationTests {

        @Test
        @DisplayName("Email válido debe pasar validación")
        void email_valid_passesValidation() {
            ClientEntity client = validClient();

            // Probar diferentes emails válidos
            String[] validEmails = {
                    "juan@toolrent.cl",
                    "maria.gonzalez@toolrent.cl",
                    "pedro+admin@toolrent.com",
                    "user123@company.co.uk"
            };

            for (String email : validEmails) {
                client.setEmail(email);
                Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);
                assertThat(violations)
                        .as("Email %s debería ser válido", email)
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("Email null debe fallar con mensaje 'obligatorio'")
        void email_null_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail(null);

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Email vacío debe fallar")
        void email_empty_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail("");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("Email sin @ debe fallar")
        void email_withoutAt_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail("juantoolrent.cl");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .containsAnyOf("email", "Email");
        }

        @Test
        @DisplayName("Email sin dominio debe fallar")
        void email_withoutDomain_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail("juan@");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Email sin usuario debe fallar")
        void email_withoutUser_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail("@toolrent.cl");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Email con formato inválido debe fallar")
        void email_invalidFormat_failsValidation() {
            ClientEntity client = validClient();
            client.setEmail("not-an-email");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
        }
    }

    // ==================== TESTS DE VALIDACIÓN: STATE ====================

    @Nested
    @DisplayName("Validación de State")
    class StateValidationTests {

        @Test
        @DisplayName("State 'Activo' debe pasar validación")
        void state_activo_passesValidation() {
            ClientEntity client = validClient();
            client.setState("Activo");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("State 'Restringido' debe pasar validación")
        void state_restringido_passesValidation() {
            ClientEntity client = validClient();
            client.setState("Restringido");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("State null debe fallar con mensaje 'obligatorio'")
        void state_null_failsValidation() {
            ClientEntity client = validClient();
            client.setState(null);

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("State vacío debe fallar con mensaje 'obligatorio'")
        void state_empty_failsValidation() {
            ClientEntity client = validClient();
            client.setState("");

            String message = getFirstViolationMessage(client);

            // Cuando está vacío, @NotBlank falla primero
            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("State con solo espacios debe fallar con mensaje 'obligatorio'")
        void state_blank_failsValidation() {
            ClientEntity client = validClient();
            client.setState("   ");

            String message = getFirstViolationMessage(client);

            assertThat(message).contains("obligatorio");
        }

        @Test
        @DisplayName("State 'Inactivo' debe fallar con mensaje de valores permitidos")
        void state_inactivo_failsValidation() {
            ClientEntity client = validClient();
            client.setState("Inactivo");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .contains("Valores permitidos: Activo, Restringido");
        }

        @Test
        @DisplayName("State 'ACTIVO' (mayúsculas) debe fallar")
        void state_uppercaseActivo_failsValidation() {
            ClientEntity client = validClient();
            client.setState("ACTIVO");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("State con valor aleatorio debe fallar")
        void state_randomValue_failsValidation() {
            ClientEntity client = validClient();
            client.setState("EstadoInvalido");

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getMessage())
                    .contains("Estado inválido");
        }
    }

    // ==================== TESTS DE GETTERS Y SETTERS ====================

    @Nested
    @DisplayName("Getters y Setters (Lombok)")
    class GettersSettersTests {

        @Test
        @DisplayName("Setters deben modificar los valores correctamente")
        void setters_modifyValues() {
            ClientEntity client = new ClientEntity();

            client.setId(10L);
            client.setName("Pedro Rojas");
            client.setRut("11.111.111-1");
            client.setPhone("+56999999999");
            client.setEmail("pedro@test.cl");
            client.setState("Restringido");

            assertThat(client.getId()).isEqualTo(10L);
            assertThat(client.getName()).isEqualTo("Pedro Rojas");
            assertThat(client.getRut()).isEqualTo("11.111.111-1");
            assertThat(client.getPhone()).isEqualTo("+56999999999");
            assertThat(client.getEmail()).isEqualTo("pedro@test.cl");
            assertThat(client.getState()).isEqualTo("Restringido");
        }

        @Test
        @DisplayName("Getters deben retornar valores asignados")
        void getters_returnAssignedValues() {
            ClientEntity client = validClient();

            assertThat(client.getId()).isNotNull();
            assertThat(client.getName()).isNotNull();
            assertThat(client.getRut()).isNotNull();
            assertThat(client.getPhone()).isNotNull();
            assertThat(client.getEmail()).isNotNull();
            assertThat(client.getState()).isNotNull();
        }
    }

    // ==================== TESTS INTEGRADOS ====================

    @Nested
    @DisplayName("Tests Integrados")
    class IntegrationTests {

        @Test
        @DisplayName("Cliente válido completo no debe tener violaciones")
        void validClient_noViolations() {
            ClientEntity client = validClient();

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Cliente con múltiples campos inválidos debe tener múltiples violaciones")
        void invalidClient_multipleViolations() {
            ClientEntity client = new ClientEntity();
            // Todos los campos null o vacíos

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            // Debe haber al menos 5 violaciones (uno por cada campo obligatorio)
            assertThat(violations.size()).isGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Cliente con datos válidos mínimos debe pasar validación")
        void minimalValidClient_passesValidation() {
            ClientEntity client = new ClientEntity(
                    null, // ID puede ser null (auto-generado)
                    "Ab", // Nombre mínimo (2 caracteres)
                    "1.234.567-K",
                    "+56900000000",
                    "a@b.cl",
                    "Activo"
            );

            Set<ConstraintViolation<ClientEntity>> violations = validator.validate(client);

            assertThat(violations).isEmpty();
        }
    }
}