package com.example.demo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("Security config bean is created")
    void securityConfigBeanExists() {
        assertThat(securityConfig).isNotNull();
    }

    @Test
    @DisplayName("CORS configuration source exists")
    void corsConfigurationSourceExists() {
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    @DisplayName("JWT authentication converter is configured")
    void jwtAuthenticationConverterIsConfigured() {
        var converter = securityConfig.jwtAuthenticationConverter();

        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("JWT converter extracts realm_access roles")
    void jwtConverterExtractsRealmRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("USER", "ADMIN")))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("JWT converter extracts resource_access roles")
    void jwtConverterExtractsResourceRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", java.util.Map.of(
                        "sisgr-backend", java.util.Map.of("roles", java.util.List.of("EMPLOYEE"))
                ))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("JWT converter handles empty roles")
    void jwtConverterHandlesEmptyRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user123")
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles null realm_access")
    void jwtConverterHandlesNullRealmAccess() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", null)
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles malformed realm_access")
    void jwtConverterHandlesMalformedRealmAccess() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", "invalid")
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles malformed resource_access")
    void jwtConverterHandlesMalformedResourceAccess() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", "invalid")
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles both realm and resource roles")
    void jwtConverterHandlesBothRoleTypes() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("USER")))
                .claim("resource_access", java.util.Map.of(
                        "sisgr-backend", java.util.Map.of("roles", java.util.List.of("EMPLOYEE"))
                ))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_USER", "ROLE_EMPLOYEE");
    }

    @Test
    @DisplayName("JWT converter handles multiple resource clients")
    void jwtConverterHandlesMultipleResourceClients() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", java.util.Map.of(
                        "client1", java.util.Map.of("roles", java.util.List.of("ROLE1")),
                        "client2", java.util.Map.of("roles", java.util.List.of("ROLE2"))
                ))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_ROLE1", "ROLE_ROLE2");
    }

    @Test
    @DisplayName("JWT converter adds ROLE_ prefix to all roles")
    void jwtConverterAddsRolePrefix() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ADMIN")))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .allMatch(role -> ((String) role).startsWith("ROLE_"));
    }

    @Test
    @DisplayName("JWT converter handles realm_access without roles key")
    void jwtConverterHandlesRealmAccessWithoutRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("other_key", "value"))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles resource_access without roles key")
    void jwtConverterHandlesResourceAccessWithoutRoles() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", java.util.Map.of(
                        "client", java.util.Map.of("other_key", "value")
                ))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles empty realm_access roles list")
    void jwtConverterHandlesEmptyRealmAccessRolesList() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of()))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles empty resource_access roles list")
    void jwtConverterHandlesEmptyResourceAccessRolesList() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("resource_access", java.util.Map.of(
                        "client", java.util.Map.of("roles", java.util.List.of())
                ))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT converter handles roles with special characters")
    void jwtConverterHandlesRolesWithSpecialCharacters() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ROLE-WITH-DASH", "role_with_underscore")))
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).extracting("authority")
                .contains("ROLE_ROLE-WITH-DASH", "ROLE_role_with_underscore");
    }

    @Test
    @DisplayName("JWT converter can be called multiple times")
    void jwtConverterCanBeCalledMultipleTimes() {
        var converter = securityConfig.jwtAuthenticationConverter();

        var jwt1 = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token1")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("USER")))
                .build();

        var jwt2 = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token2")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ADMIN")))
                .build();

        var auth1 = converter.convert(jwt1);
        var auth2 = converter.convert(jwt2);

        assertThat(auth1.getAuthorities()).extracting("authority").contains("ROLE_USER");
        assertThat(auth2.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }
}