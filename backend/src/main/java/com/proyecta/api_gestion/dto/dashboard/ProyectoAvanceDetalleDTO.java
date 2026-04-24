package com.proyecta.api_gestion.dto.dashboard;

/**
 * Proyección de Spring Data para la consulta nativa de avance detallado de un proyecto.
 * Los nombres de los getters se mapean automáticamente a los alias de la query SQL
 * (snake_case → camelCase).
 */
public interface ProyectoAvanceDetalleDTO {

    /** Avance total calculado del proyecto (0-100) */
    Integer getAvanceTotal();

    /** Cantidad de entregables en estado conforme */
    Long getEntregablesConformes();

    /** Total de entregables del proyecto */
    Long getTotalEntregables();

    /** Label combinado: conformes/total (ej: "15/20") */
    String getEntregablesLabel();

    /** Entregables no conformes cuya fecha de entrega ya pasó */
    Long getAtrasados();

    /** Entregables pendientes próximos a vencer según ventana configurada */
    Long getProximosVencer();
}
