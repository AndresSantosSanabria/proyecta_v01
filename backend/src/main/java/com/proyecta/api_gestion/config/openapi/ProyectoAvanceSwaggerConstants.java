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
    public static final String TAG_DESCRIPTION = "Consulta de métricas detalladas de avance, aprobación de entregables "
                                               + "y alertas de vencimiento para un proyecto específico";

    // --- Operación GET /{proyectoId}/avance ---
    public static final String SUMMARY_GET_AVANCE     = "Obtener avance detallado por proyecto";
    public static final String DESCRIPTION_GET_AVANCE =
            "Obtener el avance detallado del proyecto con árbol Fases > Hitos > Entregables. "
            + "El avance es calculado automáticamente a partir del estado de los entregables.";

    // --- Operación POST /{proyectoId}/avance/entregables/{entregableId}/evidencia ---
    public static final String SUMMARY_POST_EVIDENCIA     = "Subir evidencia de entregable";
    public static final String DESCRIPTION_POST_EVIDENCIA =
            "Registra el PDF de evidencia de un entregable y lo deja pendiente de aprobación por el gestor.";

    // --- Operación PATCH /{proyectoId}/avance/entregables/{entregableId}/aprobar ---
    public static final String SUMMARY_PATCH_APROBAR     = "Aprobar entregable";
    public static final String DESCRIPTION_PATCH_APROBAR =
            "Marca un entregable como 'Aprobado' después de que el gestor valide la evidencia cargada. "
            + "El avance total del proyecto se recalcula automáticamente.";

    // --- Operación PATCH /{proyectoId}/avance/entregables/{entregableId}/rechazar ---
    public static final String SUMMARY_PATCH_RECHAZAR     = "Rechazar entregable";
    public static final String DESCRIPTION_PATCH_RECHAZAR =
            "Marca un entregable como rechazado para que el asignado corrija la evidencia y la vuelva a presentar.";

    // --- Respuestas ---
    public static final String RESPONSE_200_DESC = "Métricas de avance obtenidas correctamente";
    public static final String RESPONSE_404_DESC = "El proyecto no existe o no tiene datos registrados";
}
