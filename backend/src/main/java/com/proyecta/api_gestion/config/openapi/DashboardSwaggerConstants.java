package com.proyecta.api_gestion.config.openapi;

public final class DashboardSwaggerConstants {

    private DashboardSwaggerConstants() {
        throw new UnsupportedOperationException("Esta es una clase de constantes y no puede ser instanciada");
    }

    public static final String TAG_NAME = "Dashboard";
    public static final String TAG_DESCRIPTION = "Endpoints para el resumen ejecutivo de la plataforma";

    public static final String SUMMARY_GET_SUMMARY = "Obtener resumen ejecutivo del dashboard";
    public static final String DESCRIPTION_GET_SUMMARY = "Calcula métricas agregadas de todos los proyectos activos, incluyendo avance real vs esperado, tendencias y proyectos con atrasos.";

    public static final String SUMMARY_GET_PROJECTS = "Listar resumen detallado de proyectos";
    public static final String DESCRIPTION_GET_PROJECTS = "Retorna la lista de proyectos activos con su nivel de avance, estado de cumplimiento y entregables pendientes para la tabla del dashboard.";

    public static final String RESPONSE_200_DESC = "Operación exitosa";
    public static final String RESPONSE_201_DESC = "Recurso creado exitosamente";
    public static final String RESPONSE_301_DESC = "El recurso se ha movido permanentemente";
    public static final String RESPONSE_400_DESC = "Solicitud mal formada o errores de validación";
    public static final String RESPONSE_401_DESC = "Autenticación requerida: token ausente, inválido o vencido";
    public static final String RESPONSE_403_DESC = "Privilegios insuficientes para realizar esta acción";
    public static final String RESPONSE_404_DESC = "El recurso solicitado no existe";
    public static final String RESPONSE_413_DESC = "El archivo supera el tamaño máximo permitido";
    public static final String RESPONSE_422_DESC = "La solicitud es correcta pero viola una regla de negocio (recurso en un estado que no permite la operación)";
    public static final String RESPONSE_429_DESC = "Demasiadas solicitudes desde la misma IP (rate limit de los endpoints públicos)";
    public static final String RESPONSE_500_DESC = "Error interno no controlado en el servidor";

    public static final String EXAMPLE_201 = "{\"success\": true, \"message\": \"Recurso creado con éxito\", \"timestamp\": \"2026-04-29T12:00:00\", \"data\": {}}";

    /** 400: GlobalExceptionHandler → ProblemDetails (validation-error / bad-request / missing-parameter). */
    public static final String EXAMPLE_400 =
        "{\"type\":\"/errors/validation-error\",\"title\":\"Error de validación\",\"status\":400,\"detail\":\"Error de validación: nombre: debe tener entre 3 y 120 caracteres, responsableId: debe ser un UUID válido\"}";

    /** 401: JwtAuthenticationEntryPoint → mapa con action (único formato 401 real de la API). */
    public static final String EXAMPLE_401 =
        "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token de acceso ausente, inválido o vencido. Renueve el token o inicie sesión nuevamente.\",\"path\":\"/api/v1/proyectos/PROY-CUN-2026-008\",\"timestamp\":\"2026-04-29T12:00:00\",\"action\":\"REFRESH_OR_LOGIN\"}";

    /** 403: ForbiddenException (propiedad del recurso) o AccessDeniedException (@PreAuthorize). */
    public static final String EXAMPLE_403 =
        "{\"type\":\"/errors/access-denied\",\"title\":\"Acceso prohibido\",\"status\":403,\"detail\":\"No tiene permisos para realizar esta acción.\"}";

    /** 404: ResourceNotFoundException devuelto por el GlobalExceptionHandler. */
    public static final String EXAMPLE_404 =
        "{\"type\":\"/errors/not-found\",\"title\":\"Recurso no encontrado\",\"status\":404,\"detail\":\"Proyecto no encontrado: PROY-CUN-2026-999\"}";

    /** 404 de los endpoints públicos: PublicEvidenceSecurityFilter responde genérico (sin oráculo de existencia). */
    public static final String EXAMPLE_404_PUBLIC =
        "{\"message\":\"Recurso no encontrado\"}";

    /** 413: MaxUploadSizeExceededException en cargas multipart. */
    public static final String EXAMPLE_413 =
        "{\"type\":\"/errors/file-too-large\",\"title\":\"Archivo demasiado grande\",\"status\":413,\"detail\":\"El archivo excede el tamaño máximo permitido.\"}";

    /** 422: UnprocessableEntityException (reglas de negocio) en operaciones de escritura. */
    public static final String EXAMPLE_422 =
        "{\"type\":\"/errors/unprocessable-entity\",\"title\":\"Entidad no procesable\",\"status\":422,\"detail\":\"El proyecto PROY-CUN-2026-008 ya fue cerrado; no admite cambios\"}";

    /** 429: PublicEvidenceSecurityFilter (rate limit por IP) con cabecera Retry-After: 60. */
    public static final String EXAMPLE_429 =
        "{\"message\":\"Demasiadas solicitudes. Intente mas tarde.\"}";

    /** 500: GlobalExceptionHandler (el detalle real no se expone por CWE-209). */
    public static final String EXAMPLE_500 =
        "{\"type\":\"/errors/internal-error\",\"title\":\"Error interno del servidor\",\"status\":500,\"detail\":\"Error interno del servidor. Contacte al administrador.\"}";
}
