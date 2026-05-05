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
            "Obtener el avance detallado del proyecto con árbol Fases > Hitos > Entregables. "
            + "El avance es calculado automáticamente a partir del estado de los entregables.";

    // --- Operación PATCH /{proyectoId}/avance/entregables/{entregableId} ---
    public static final String SUMMARY_PATCH_AVANCE     = "Marcar entregable como conforme";
    public static final String DESCRIPTION_PATCH_AVANCE = 
            "Marca un entregable como 'A conformidad' y permite subir el archivo PDF de evidencia. "
            + "El avance total del proyecto se recalcula automáticamente.";

    // --- Respuestas ---
    public static final String RESPONSE_200_DESC = "Métricas de avance obtenidas correctamente";
    public static final String RESPONSE_404_DESC = "El proyecto no existe o no tiene datos registrados";
}
