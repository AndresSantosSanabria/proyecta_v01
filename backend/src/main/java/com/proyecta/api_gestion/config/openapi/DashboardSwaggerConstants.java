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

    public static final String SUMMARY_GET_PROJECT_BY_ID = "Obtener resumen detallado de un proyecto por ID";
    public static final String DESCRIPTION_GET_PROJECT_BY_ID = "Calcula métricas específicas para un proyecto (avance, ratio de entregables, atrasos y alertas de vencimiento) usando el parámetro de ventana configurado.";

    public static final String RESPONSE_200_DESC = "Operación exitosa";
    public static final String RESPONSE_201_DESC = "Recurso creado exitosamente";
    public static final String RESPONSE_301_DESC = "El recurso se ha movido permanentemente";
    public static final String RESPONSE_400_DESC = "Solicitud mal formada o errores de validación";
    public static final String RESPONSE_401_DESC = "Autenticación requerida o token inválido";
    public static final String RESPONSE_403_DESC = "Privilegios insuficientes para realizar esta acción";
    public static final String RESPONSE_404_DESC = "El recurso solicitado no existe";
    public static final String RESPONSE_500_DESC = "Error interno no controlado en el servidor";
    public static final String RESPONSE_503_DESC = "Servicio temporalmente fuera de servicio (Mantenimiento o caída de BD)";

    public static final String EXAMPLE_201 = "{\"success\": true, \"message\": \"Recurso creado con éxito\", \"timestamp\": \"2026-04-29T12:00:00\", \"data\": {}}";
    public static final String EXAMPLE_400 = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validación fallida: campo X es obligatorio\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
    public static final String EXAMPLE_401 = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token ausente o inválido\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
    public static final String EXAMPLE_403 = "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"No tiene permisos suficientes\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
    public static final String EXAMPLE_404 = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"Recurso no encontrado\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
    public static final String EXAMPLE_500 = "{\"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Error inesperado\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
    public static final String EXAMPLE_503 = "{\"status\": 503, \"error\": \"Service Unavailable\", \"message\": \"Servicio de base de datos caído o en mantenimiento\", \"path\": \"/api/...\", \"timestamp\": \"2026-04-29T12:00:00\"}";
}
