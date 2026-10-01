package com.proyecta.api_gestion.config.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un controlador o método cuyo endpoint es accesible sin autenticación
 * (rutas {@code permitAll} en {@code SecurityConfig}, p. ej. {@code /api/v1/public/**}).
 *
 * <p>{@link ErrorResponseOperationCustomizer} la usa para:</p>
 * <ul>
 *   <li>excluir la seguridad global JWT ({@code security: []} en la operación),</li>
 *   <li>documentar únicamente los errores que realmente aplican (400/404/429/500)
 *       en lugar de 401/403/422.</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PublicEndpoint {
}
