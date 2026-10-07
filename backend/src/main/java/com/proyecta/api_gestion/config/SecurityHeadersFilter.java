package com.proyecta.api_gestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeceras de seguridad globales (CWE-693 / OWASP A05).
 *
 * - X-Content-Type-Options: nosniff  -> impide que el navegador reinterprete
 *   descargas (PDF/XLSX) como HTML/JS (CWE-693 / CWE-79).
 * - X-Frame-Options: SAMEORIGIN      -> anti-clickjacking; SAMEORIGIN para no
 *   romper los visores <object>/<iframe> de la propia SPA (incluidos blob:).
 * - Referrer-Policy / Permissions-Policy -> reduce fuga de metadatos.
 *
 * No se añade Content-Security-Policy global porque la SPA carga recursos
 * inline/blob; si se desea, debe aplicarse por perfil de respuesta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        filterChain.doFilter(request, response);
    }
}
