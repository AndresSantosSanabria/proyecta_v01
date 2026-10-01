package com.proyecta.api_gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import com.proyecta.api_gestion.service.security.dynamic.DynamicJwtAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * Security Configuration for Proyecta API
 * Follows SOLID principles:
 * - SRP: This class is only responsible for configuring the security filter chain.
 * - DIP: Depends on abstractions (JwtAuthenticationConverter) to map roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> allowedOrigins;

    public SecurityConfig(
            @org.springframework.beans.factory.annotation.Value("${gob.security.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    /**
     * Registra el filtro de evidencia pública ANTES de la cadena de seguridad de Spring
     * (orden por debajo de -100): rate limiting por IP y validación de firma HMAC en
     * /api/v1/public/** sin exigir sesión (requisito de negocio anti-enumeración).
     */
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<PublicEvidenceSecurityFilter>
            publicEvidenceSecurityFilterRegistration(
                    com.proyecta.api_gestion.service.PublicEvidenceUrlSigner signer,
                    @org.springframework.beans.factory.annotation.Value(
                            "${gob.security.public-evidence.rate-limit-per-minute:120}") int rateLimitPerMinute,
                    @org.springframework.beans.factory.annotation.Value(
                            "${gob.security.public-evidence.trust-forwarded-for:false}") boolean trustForwardedFor) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(
                new PublicEvidenceSecurityFilter(signer, rateLimitPerMinute, trustForwardedFor));
        registration.addUrlPatterns("/api/v1/public/*");
        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            UserProvisioningFilter userProvisioningFilter,
            SystemAuditFilter systemAuditFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/api/public/**",
                    "/api/v1/public/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(systemAuditFilter, UserProvisioningFilter.class);

        org.springframework.security.web.SecurityFilterChain chain = http.build();

        // El AuthenticationEntryPointFailureHandler por defecto RELANZA las
        // AuthenticationServiceException (p.ej. cuando IAM no responde al validar el
        // JWKS), lo que escapa hasta Tomcat e imprime un stack de ~80 lineas por
        // peticion. Se configura para responder 401 con el entry point propio.
        org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler failureHandler =
                new org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler(jwtAuthenticationEntryPoint);
        failureHandler.setRethrowAuthenticationServiceException(false);
        for (jakarta.servlet.Filter filter : chain.getFilters()) {
            if (filter instanceof BearerTokenAuthenticationFilter bearerFilter) {
                bearerFilter.setAuthenticationFailureHandler(failureHandler);
            }
        }

        return chain;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setExposedHeaders(Arrays.asList("Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(DynamicJwtAuthoritiesConverter authoritiesConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
