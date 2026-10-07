package com.proyecta.api_gestion.config.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Definición global de OpenAPI 3.0 (Swagger) para la API de Proyecta.
 *
 * <p>Contiene la información general, los servidores, el esquema de seguridad
 * JWT Bearer y el requerimiento de seguridad global. Los endpoints anónimos
 * (anotados con {@link PublicEndpoint}) quedan exentos mediante
 * {@link ErrorResponseOperationCustomizer}, que fija {@code security: []}
 * a nivel de operación.</p>
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Proyecta — API de Gestión de Proyectos TIC",
        version = "1.0.0",
        description = """
            API REST de Proyecta: gestión de proyectos, cronograma, entregables, riesgos, \
            evidencias, documentos, reportes, cierre y notificaciones.

            **Autenticación:** la mayoría de los endpoints requieren un JWT emitido por Keycloak \
            (se obtiene al iniciar sesión en la aplicación). Incluirlo en la cabecera \
            `Authorization: Bearer <token>`. Use el botón <b>Authorize</b> de Swagger UI para \
            probar los endpoints autenticados.

            **Endpoints públicos:** `/api/v1/public/**` no requiere token; están protegidos con \
            rate limit por IP (429) y firma HMAC o token opaco (404 si la firma no es válida).

            **Errores:** los errores de negocio siguen Problem Details (RFC 9457) con el formato \
            `{type, title, status, detail}`. El 401 de token ausente/inválido devuelve \
            `{status, error, message, path, timestamp, action}`.
            """
    ),
    servers = {
        @Server(url = "/", description = "Servidor actual (host del despliegue)"),
        @Server(url = "http://localhost:${server.port:8082}", description = "Local (desarrollo)")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT emitido por Keycloak al iniciar sesión en Proyecta"
)
public class OpenApiConfig {
}
