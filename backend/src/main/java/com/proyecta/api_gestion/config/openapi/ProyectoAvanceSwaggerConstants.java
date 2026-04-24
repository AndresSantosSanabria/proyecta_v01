package com.proyecta.api_gestion.config.openapi;

/**
 * Constantes Swagger para el módulo de Avance Detallado de Proyecto.
 */
public final class ProyectoAvanceSwaggerConstants {

    private ProyectoAvanceSwaggerConstants() {
        throw new UnsupportedOperationException("Clase de constantes — no instanciable");
    }

    // --- Tag ---
    public static final String TAG_NAME        = "Avance de Proyecto";
    public static final String TAG_DESCRIPTION = "Consulta de métricas detalladas de avance, conformidad de entregables "
                                               + "y alertas de vencimiento para un proyecto específico";

    // --- Operación GET /{proyectoId}/avance ---
    public static final String SUMMARY_GET_AVANCE     = "Obtener avance detallado por proyecto";
    public static final String DESCRIPTION_GET_AVANCE =
            "Ejecuta un análisis completo del proyecto indicado y retorna:\n"
            + "- **Avance total** calculado (0-100 %)\n"
            + "- **Entregables conformes** vs total (label 'conformes/total')\n"
            + "- **Entregables atrasados** (no conformes con fecha_entrega < hoy)\n"
            + "- **Próximos a vencer** (pendientes en la ventana configurada en `system_parameters`)\n\n"
            + "Retorna **404** si el proyectoId no existe en la base de datos.";

    // --- Respuestas ---
    public static final String RESPONSE_200_DESC = "Métricas de avance obtenidas correctamente";
    public static final String RESPONSE_404_DESC = "El proyecto no existe o no tiene datos registrados";
}
