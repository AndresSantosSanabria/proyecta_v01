package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.service.security.UserProvisioningService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de aprovisionamiento JIT (Just-In-Time).
 *
 * Se ejecuta en cada petición autenticada con JWT.
 * Delega la lógica de negocio al UserProvisioningService que usa
 * REQUIRES_NEW para garantizar el commit independiente de la transacción.
 *
 * Posicionado DESPUÉS de BearerTokenAuthenticationFilter para asegurarse
 * de que el SecurityContext ya contiene el Jwt validado.
 */
@Component
public class UserProvisioningFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningFilter.class);

    private final UserProvisioningService userProvisioningService;

    public UserProvisioningFilter(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt) {
            try {
                userProvisioningService.upsert(authentication);
            } catch (Exception e) {
                // Logueamos el stack trace completo para diagnóstico, pero NO
                // bloqueamos la petición — el usuario autenticado puede seguir.
                log.error("[JIT] Error al provisionar usuario autenticado: {}", e.getMessage(), e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
