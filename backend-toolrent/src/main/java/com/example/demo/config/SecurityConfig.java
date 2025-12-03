package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;

import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // Esto busca el bean corsConfigurationSource
                .authorizeHttpRequests(auth -> auth
                        // 🔥 CRÍTICO: Permitir TODOS los OPTIONS sin token
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos (Swagger, etc)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // El resto requiere autenticación
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> out = new ArrayList<>();
            Object realmAccessObj = jwt.getClaims().get("realm_access");
            if (realmAccessObj instanceof Map<?, ?> realmAccess) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof Collection<?> roles) {
                    roles.forEach(r -> out.add(new SimpleGrantedAuthority("ROLE_" + String.valueOf(r))));
                }
            }
            Object resourceAccessObj = jwt.getClaims().get("resource_access");
            if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
                for (Object v : resourceAccess.values()) {
                    if (v instanceof Map<?, ?> client) {
                        Object clientRolesObj = client.get("roles");
                        if (clientRolesObj instanceof Collection<?> clientRoles) {
                            clientRoles.forEach(r -> out.add(new SimpleGrantedAuthority("ROLE_" + String.valueOf(r))));
                        }
                    }
                }
            }
            return out;
        });
        return converter;
    }

    /**
     * 🔥 CONFIGURACIÓN CORS QUE PERMITE TODO DESDE DOCKER
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Usamos AllowedOriginPatterns en lugar de AllowedOrigins
        // Esto permite que el navegador envíe credenciales (cookies/tokens) desde CUALQUIER origen
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}