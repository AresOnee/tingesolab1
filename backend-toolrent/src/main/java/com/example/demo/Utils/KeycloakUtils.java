package com.example.demo.Utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * ✅ CLASE UTILITARIA PARA KEYCLOAK
 *
 * Centraliza la lógica de extracción de información del JWT de Keycloak.
 *
 * Uso:
 *   String username = KeycloakUtils.getUsername(authentication);
 *
 * Ventajas:
 *   - Un solo lugar para mantener la lógica
 *   - Fácil de testear
 *   - Reutilizable en todos los controladores
 */
public class KeycloakUtils {

    /**
     * Extrae el username real del usuario autenticado en Keycloak.
     *
     * Prioridad de extracción:
     * 1. preferred_username (campo estándar de Keycloak)
     * 2. name (nombre completo)
     * 3. sub (UUID del usuario)
     * 4. authentication.getName() (fallback)
     * 5. "SYSTEM" (fallback final)
     *
     * @param authentication Objeto de autenticación de Spring Security
     * @return Username del usuario, o "SYSTEM" si no se puede determinar
     */
    public static String getUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Prioridad 1: preferred_username (estándar de Keycloak)
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isEmpty()) {
                return preferredUsername;
            }

            // Prioridad 2: name (nombre completo)
            String name = jwt.getClaimAsString("name");
            if (name != null && !name.isEmpty()) {
                return name;
            }

            // Prioridad 3: sub (UUID único del usuario)
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.isEmpty()) {
                return sub;
            }
        }

        // Prioridad 4: Fallback a getName() (para tests con @WithMockUser)
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        // Prioridad 5: Fallback final
        return "SYSTEM";
    }

    /**
     * Obtiene el email del usuario autenticado (si está disponible).
     *
     * @param authentication Objeto de autenticación
     * @return Email del usuario, o null si no está disponible
     */
    public static String getEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return null;
    }

    /**
     * Obtiene el nombre completo del usuario autenticado.
     *
     * @param authentication Objeto de autenticación
     * @return Nombre completo, o null si no está disponible
     */
    public static String getFullName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("name");
        }
        return null;
    }

    /**
     * Verifica si el usuario tiene un rol específico.
     *
     * @param authentication Objeto de autenticación
     * @param role Nombre del rol (ej: "ADMIN")
     * @return true si el usuario tiene el rol, false en caso contrario
     */
    public static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}