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

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest; // <- IMPORTANTE

import java.util.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Cadena #0: SOLO endpoints de Actuator → públicos */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())   // match a /actuator/**
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    /** Cadena #1: Resto de la app → protegida con JWT */
    @Bean
    @Order(1)
    public SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Swagger si lo usas (opcional):
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /** Convierte roles de Keycloak (realm + clientes) a ROLE_* */
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

    /** CORS para el front en Vite y Docker */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",      // Frontend desarrollo (npm run dev)
                "http://127.0.0.1:5173",      // Frontend desarrollo (IP)
                "http://localhost",           // Frontend Docker (puerto 80)
                "http://127.0.0.1",            // Frontend Docker (IP puerto 80)
                "http://localhost:8090", // <--- Añadir puerto del backend
                "http://127.0.0.1:8090", // <--- Añadir IP del backend
                "http://0.0.0.0:8090",   // <--- Añadir 0.0.0.0
                "http://0.0.0.0"         // <--- Añadir 0.0.0.0
        ));
        c.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        c.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","Cache-Control","X-Requested-With"));
        c.setExposedHeaders(Arrays.asList("Authorization"));
        c.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
