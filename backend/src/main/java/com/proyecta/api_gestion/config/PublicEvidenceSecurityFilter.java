package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.service.PublicEvidenceUrlSigner;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Endurece los endpoints públicos anónimos de /api/v1/public/** (requisito: acceso sin
 * login) con dos controles de defensa perimetral:
 *
 * 1. Rate limiting por IP (CWE-307): frena la enumeración automatizada de IDs secuenciales
 *    aunque un atacante conozca la estructura de los enlaces.
 * 2. Validación de firma HMAC (?exp=&sig=) en todos los recursos salvo /evidencia/{token},
 *    que ya usa un token opaco de 256 bits (CWE-639 / OWASP A01).
 *
 * Toda respuesta denegada se devuelve como 404 genérico (sin oráculo de existencia) y se
 * audita IP + recurso + resultado (CWE-778).
 */
public class PublicEvidenceSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublicEvidenceSecurityFilter.class);

    private static final String PUBLIC_PREFIX = "/api/v1/public/";
    /** Endpoint ya protegido con token opaco de 256 bits: exento de firma HMAC. */
    private static final String TOKEN_EXEMPT_PREFIX = "/api/v1/public/evidencia/";
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_IPS = 50_000;

    private final PublicEvidenceUrlSigner signer;
    private final int rateLimitPerMinute;
    private final boolean trustForwardedFor;
    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    public PublicEvidenceSecurityFilter(PublicEvidenceUrlSigner signer,
                                        int rateLimitPerMinute,
                                        boolean trustForwardedFor) {
        this.signer = signer;
        this.rateLimitPerMinute = Math.max(1, rateLimitPerMinute);
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri == null || !uri.contains(PUBLIC_PREFIX) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);

        if (!allow(clientIp)) {
            log.warn("PUBLIC_RATE_LIMITED ip={} path={}", clientIp, uri);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intente mas tarde.\"}");
            return;
        }

        if (uri.startsWith(TOKEN_EXEMPT_PREFIX)) {
            log.info("PUBLIC_ACCESS ip={} path={} result=OK_TOKEN", clientIp, uri);
            filterChain.doFilter(request, response);
            return;
        }

        String exp = request.getParameter("exp");
        String sig = request.getParameter("sig");
        if (!signer.isValid(uri, exp, sig)) {
            log.warn("PUBLIC_ACCESS_DENIED ip={} path={} reason=SIGNATURE_INVALID", clientIp, uri);
            writeNotFound(response);
            return;
        }

        log.info("PUBLIC_ACCESS ip={} path={} result=OK_SIGNED", clientIp, uri);
        filterChain.doFilter(request, response);
    }

    private void writeNotFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Recurso no encontrado\"}");
    }

    private boolean allow(String ip) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startMs >= WINDOW_MILLIS);
        if (windows.size() >= MAX_TRACKED_IPS && !windows.containsKey(ip)) {
            return false;
        }
        RateWindow window = windows.compute(ip, (key, existing) ->
                (existing == null || now - existing.startMs >= WINDOW_MILLIS) ? new RateWindow(now) : existing);
        return window.count.incrementAndGet() <= rateLimitPerMinute;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static final class RateWindow {
        private final long startMs;
        private final AtomicInteger count = new AtomicInteger();

        private RateWindow(long startMs) {
            this.startMs = startMs;
        }
    }
}
