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
    public static final String RESPONSE_400_DESC = "Parámetros de consulta inválidos";
    public static final String RESPONSE_401_DESC = "Token de autenticación ausente o inválido";
    public static final String RESPONSE_403_DESC = "No tiene permisos para acceder a este recurso";
    public static final String RESPONSE_404_DESC = "No se encontraron datos para generar el resumen";
    public static final String RESPONSE_500_DESC = "Error inesperado al calcular las métricas";
    public static final String RESPONSE_503_DESC = "El servicio de base de datos no está disponible actualmente";

    public static final String EXAMPLE_400 = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"Validación fallida: campo X es obligatorio\", \"path\": \"/api/dashboard/summary\"}";
    public static final String EXAMPLE_401 = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token ausente o inválido\", \"path\": \"/api/dashboard/summary\"}";
    public static final String EXAMPLE_403 = "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"Sin permisos para el recurso\", \"path\": \"/api/dashboard/summary\"}";
    public static final String EXAMPLE_404 = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"Recurso no encontrado\", \"path\": \"/api/dashboard/summary\"}";
    public static final String EXAMPLE_500 = "{\"status\": 500, \"error\": \"Internal Server Error\", \"message\": \"Error inesperado\", \"path\": \"/api/dashboard/summary\"}";
    public static final String EXAMPLE_503 = "{\"status\": 503, \"error\": \"Service Unavailable\", \"message\": \"Servicio de base de datos caído\", \"path\": \"/api/dashboard/summary\"}";
}
