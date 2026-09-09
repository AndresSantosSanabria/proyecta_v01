package com.proyecta.api_gestion.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecta.api_gestion.model.audit.SystemAuditLog;
import com.proyecta.api_gestion.service.audit.SystemAuditLogService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtro de auditorÃ­a de peticiones HTTP.
 *
 * Intercepta TODAS las peticiones (a diferencia de AOP, no depende del
 * paquete de controllers) de forma asÃ­ncrona, registrando el usuario, rol,
 * recurso, mÃ©todo, cÃ³digo de estado y duraciÃ³n en la tabla system_audit_log.
 */
@Component
public class SystemAuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SystemAuditFilter.class);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "contrasena", "token", "secret",
            "accessToken", "refreshToken", "access_token", "refresh_token",
            "newPassword", "nuevaContrasena", "currentPassword", "contrasenaActual"
    );

    private static final Map<String, String> ENTITY_PATH_SEGMENTS = Map.ofEntries(
            Map.entry("proyectos", "Proyecto"),
            Map.entry("fases", "Fase"),
            Map.entry("hitos", "Hito"),
            Map.entry("entregables", "Entregable"),
            Map.entry("riesgos", "Riesgo"),
            Map.entry("actas-cierre", "ActaCierre"),
            Map.entry("documentos", "Documento"),
            Map.entry("patrocinadores", "Patrocinador"),
            Map.entry("usuarios", "Usuario"),
            Map.entry("roles", "SeguridadRol"),
            Map.entry("permisos", "SeguridadPermiso"),
            Map.entry("notificaciones", "Notificacion"),
            Map.entry("reportes", "ReporteConfig"),
            Map.entry("parametros", "SystemParameter"),
            Map.entry("respuestas-furag", "FuragRespuesta"),
            Map.entry("beneficios-impacto", "ProyectoBeneficioImpacto"),
            Map.entry("auditoria", "SystemAuditLog"),
            Map.entry("cambios-fecha", "EntregableCambioFecha"),
            Map.entry("cambios-descripcion", "EntregableCambioDescripcion"),
            Map.entry("versiones", "DocumentoVersion"),
            Map.entry("observaciones", "DocumentoObservacion")
    );

    private final SystemAuditLogService auditLogService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final ObjectMapper objectMapper;

    public SystemAuditFilter(SystemAuditLogService auditLogService,
                             KeycloakIdentityExtractor identityExtractor,
                             ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.identityExtractor = identityExtractor;
        this.objectMapper = objectMapper;
    }

@Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long inicio = System.currentTimeMillis();

        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
        String requestBody = null;
        String method = request.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            requestBody = captureRequestBody(wrappedRequest);
        }
        org.springframework.web.util.ContentCachingResponseWrapper wrappedResponse =
                new org.springframework.web.util.ContentCachingResponseWrapper(response);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } catch (Exception ex) {
                String respuestaBody = captureResponseBody(wrappedResponse);
                registrar(request, authentication, wrappedResponse.getStatus(), inicio, ex, requestBody, respuestaBody);
                wrappedResponse.copyBodyToResponse();
                throw ex;
            }
            String respuestaBody = captureResponseBody(wrappedResponse);
            registrar(request, authentication, wrappedResponse.getStatus(), inicio, null, requestBody, respuestaBody);
            wrappedResponse.copyBodyToResponse();
        } else {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

private String captureRequestBody(HttpServletRequest request) {
        try {
            byte[] body = request instanceof CachedBodyRequestWrapper wrappedRequest
                    ? wrappedRequest.getCachedBody()
                    : new byte[0];
            if (body != null && body.length > 0) {
                String contentType = request.getContentType();
                if (contentType != null && contentType.contains("application/json")) {
                    String bodyStr = new String(body, StandardCharsets.UTF_8);
                    return sanitizar(bodyStr);
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo capturar el body del request: {}", e.getMessage());
        }
        return null;
    }

    private String captureResponseBody(org.springframework.web.util.ContentCachingResponseWrapper response) {
        try {
            byte[] body = response.getContentAsByteArray();
            if (body != null && body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                if (bodyStr.length() > 4000) {
                    bodyStr = bodyStr.substring(0, 4000) + "...[truncado]";
                }
                return bodyStr;
            }
        } catch (Exception e) {
            log.debug("No se pudo capturar el body de la respuesta: {}", e.getMessage());
        }
        return null;
    }

    private String[] extractEntidadFromUri(String uri) {
        if (uri == null || uri.isBlank()) return new String[]{null, null};
        String[] segments = uri.split("/");
        String entidadTipo = null;
        String entidadId = null;
        for (int i = 0; i < segments.length; i++) {
            String mapped = ENTITY_PATH_SEGMENTS.get(segments[i]);
            if (mapped != null) {
                entidadTipo = mapped;
                if (i + 1 < segments.length) {
                    String candidate = segments[i + 1];
                    if (looksLikeUuid(candidate)) {
                        entidadId = candidate;
                    }
                }
                break;
            }
        }
        return new String[]{entidadTipo, entidadId};
    }

    private boolean looksLikeUuid(String value) {
        if (value == null || value.length() < 20) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

private void registrar(HttpServletRequest request,
                           Authentication authentication,
                           int codigoEstado,
                           long inicio,
                           Exception error,
                           String requestBody,
                           String respuestaBody) {
        try {
            long duracion = System.currentTimeMillis() - inicio;
            String metodoHttp = request.getMethod();
            String modulo = request.getRequestURI();
            String accion = mapearAccion(metodoHttp, modulo);
            boolean esError = error != null || codigoEstado >= 400;

            String[] entidadInfo = extractEntidadFromUri(modulo);

            String trazaError;
            if (error != null) {
                trazaError = extractDetailedError(error);
            } else if (codigoEstado >= 400 && respuestaBody != null && !respuestaBody.isBlank()) {
                trazaError = buildErrorFromResponse(respuestaBody, codigoEstado, metodoHttp, modulo);
            } else {
                trazaError = null;
            }

            SystemAuditLog entry = new SystemAuditLog();
            entry.setUsuarioId(resolveUsername(authentication));
            entry.setUsuarioNombre(resolveDisplayName(authentication));
            entry.setUsuarioRol(resolveUserRole(authentication));
            entry.setAccion(accion);
            entry.setModulo(truncar(modulo, 150));
            entry.setMetodoHttp(metodoHttp);
            entry.setRecurso(extractRecurso(request));
            entry.setCodigoEstado(codigoEstado);
            entry.setEstado(esError ? "ERROR" : "SUCCESS");
            entry.setDetalle(truncar(buildDetalle(request, codigoEstado), 4000));
            entry.setTrazaError(truncar(trazaError, 8000));
            entry.setIpOrigen(resolveIp(request));
            entry.setUserAgent(truncar(request.getHeader("User-Agent"), 500));
            entry.setRequestBody(truncar(requestBody, 4000));
            entry.setEntidadTipo(entidadInfo[0]);
            entry.setEntidadId(entidadInfo[1]);
            entry.setRespuestaBody(truncar(respuestaBody, 4000));
            entry.setDuracionMs(duracion);
            entry.setEliminado(false);

            auditLogService.registrar(entry);
        } catch (Exception ex) {
            log.error("Error registrando auditoria: {}", ex.getMessage());
        }
    }

    private String mapearAccion(String metodoHttp, String modulo) {
        if (modulo != null && (modulo.contains("/login") || modulo.contains("/logout"))) {
            return "LOGIN";
        }
        return switch (metodoHttp.toUpperCase()) {
            case "GET" -> "CONSULTA";
            case "POST" -> "CREACION";
            case "PUT", "PATCH" -> "ACTUALIZACION";
            case "DELETE" -> "ELIMINACION";
            default -> "OTRO";
        };
    }

    private String extractRecurso(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) return null;
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            String seg = segments[i];
            if (seg.length() > 3 && seg.length() < 50 && seg.matches("[A-Za-z0-9\\-]+")) {
                return seg;
            }
        }
        return null;
    }

private String buildDetalle(HttpServletRequest request, int codigoEstado) {
        if (request == null) return null;
        String metodo = request.getMethod();
        String uri = request.getRequestURI();
        StringBuilder sb = new StringBuilder();
        if (metodo != null) sb.append(metodo).append(" ");
        if (uri != null) sb.append(uri);
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            sb.append("?").append(sanitizar(query));
        }
        sb.append(" -> ").append(codigoEstado);
        return sb.toString();
    }

    private String sanitizar(String value) {
        if (value == null) return null;
        String result = value;
        for (String field : SENSITIVE_FIELDS) {
            result = result.replaceAll(
                    "(?i)" + Pattern.quote(field) + "=[^&]*",
                    field + "=***REDACTED***");
        }
        return result.length() > 4000 ? result.substring(0, 4000) + "...[truncado]" : result;
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null) return "SYSTEM";
        String username = identityExtractor.resolveUsername(authentication);
        return username != null ? username : "SYSTEM";
    }

    private String resolveDisplayName(Authentication authentication) {
        if (authentication == null) return "SYSTEM";
        String name = identityExtractor.resolveDisplayName(authentication);
        return name != null ? name : resolveUsername(authentication);
    }

    private String resolveUserRole(Authentication authentication) {
        if (authentication == null) return "SYSTEM";
        if (authentication.getAuthorities() == null) return "unknown";
        for (var auth : authentication.getAuthorities()) {
            String authority = auth.getAuthority();
            if (authority != null && authority.startsWith("ROLE_")) {
                return authority.substring(5);
            }
        }
        String username = identityExtractor.resolveUsername(authentication);
        return username != null ? "authenticated" : "SYSTEM";
    }

private String extractDetailedError(Exception ex) {
        StringBuilder sb = new StringBuilder();

        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (ex instanceof org.hibernate.exception.ConstraintViolationException cve) {
            sb.append("Constraint Violation: ");
            sb.append(cve.getConstraintName());
            sb.append(" - ").append(cve.getMessage());
        } else if (ex instanceof org.springframework.dao.DataIntegrityViolationException dive) {
            sb.append("Data Integrity: ");
            sb.append(dive.getMessage());
        } else {
            sb.append("Exception: ").append(ex.getClass().getName()).append(": ").append(ex.getMessage());
        }

        if (rootCause != ex && rootCause.getMessage() != null) {
            sb.append("\n\nCausa raiz: ").append(rootCause.getClass().getSimpleName()).append(": ").append(rootCause.getMessage());
        }

        sb.append("\n\nStack Trace Tecnico:\n");
        StackTraceElement[] trace = ex.getStackTrace();
        int limit = Math.min(trace.length, 20);
        for (int i = 0; i < limit; i++) {
            sb.append("#").append(i).append(" ").append(trace[i].toString()).append("\n");
        }
        if (trace.length > 20) {
            sb.append("... ").append(trace.length - 20).append(" frames mas");
        }

        return sb.toString();
    }

    private String buildErrorFromResponse(String responseBody, int statusCode, String metodo, String uri) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP ").append(statusCode).append(" - ").append(getHttpStatusDescription(statusCode)).append("\n");
        sb.append("Metodo: ").append(metodo).append("\n");
        sb.append("Recurso: ").append(uri).append("\n");

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String title = root.has("title") ? root.get("title").asText() : null;
            String detail = root.has("detail") ? root.get("detail").asText() : null;
            String type = root.has("type") ? root.get("type").asText() : null;
            JsonNode errors = root.get("errors");

            if (title != null) {
                sb.append("\nTipo de error: ").append(title);
            }
            if (detail != null) {
                sb.append("\nDetalle: ").append(detail);
            }
            if (type != null) {
                sb.append("\nCategoria: ").append(type);
            }
            if (errors != null && errors.isObject()) {
                sb.append("\n\nCampos con error:");
                errors.fields().forEachRemaining(field -> {
                    sb.append("\n  - ").append(field.getKey()).append(": ");
                    if (field.getValue().isArray()) {
                        field.getValue().forEach(v -> sb.append(v.asText()).append(" "));
                    } else {
                        sb.append(field.getValue().asText());
                    }
                });
            } else if (errors != null && errors.isArray()) {
                sb.append("\n\nErrores de validacion:");
                errors.forEach(err -> {
                    if (err.isObject()) {
                        String field = err.has("field") ? err.get("field").asText() : "";
                        String message = err.has("message") ? err.get("message").asText() : err.asText();
                        sb.append("\n  - ").append(field).append(": ").append(message);
                    } else {
                        sb.append("\n  - ").append(err.asText());
                    }
                });
            }
        } catch (Exception e) {
            sb.append("\nRespuesta: ").append(responseBody);
        }

        return sb.toString();
    }

    private String getHttpStatusDescription(int code) {
        return switch (code) {
            case 400 -> "Solicitud incorrecta (Bad Request)";
            case 401 -> "No autenticado (Unauthorized)";
            case 403 -> "Acceso denegado (Forbidden)";
            case 404 -> "Recurso no encontrado (Not Found)";
            case 405 -> "Metodo no permitido (Method Not Allowed)";
            case 409 -> "Conflicto (Conflict)";
            case 422 -> "Entidad no procesable (Unprocessable Entity)";
            case 429 -> "Demasiadas solicitudes (Too Many Requests)";
            case 500 -> "Error interno del servidor (Internal Server Error)";
            case 502 -> "Puerta de entrada incorrecta (Bad Gateway)";
            case 503 -> "Servicio no disponible (Service Unavailable)";
            default -> "Error HTTP " + code;
        };
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncar(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private byte[] cachedBody;
        private final boolean multipart;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            String contentType = request.getContentType();
            this.multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
            if (multipart) {
                // No consumir el stream en peticiones multipart/form-data para
                // no romper la resolucion de @RequestPart en los controllers.
                this.cachedBody = new byte[0];
            } else {
                // Leer y cachear el body
                java.io.InputStream inputStream = request.getInputStream();
                this.cachedBody = inputStream.readAllBytes();
            }
        }

        byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (multipart) {
                return super.getInputStream();
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // No-op
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }
    }
}


