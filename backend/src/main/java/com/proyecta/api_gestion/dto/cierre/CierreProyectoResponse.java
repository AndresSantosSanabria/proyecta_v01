package com.proyecta.api_gestion.dto.cierre;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record representing the response for a project closure request.
 * Leverages Java 25 record features.
 * 
 * @param success Indicates if the closure was successful.
 * @param message Success or error message.
 * @param errorBanner Detailed error message for the UI if validations failed.
 * @param fechaCierre The official closure date and time.
 * @param avanceFinal The calculated final progress percentage.
 */
public record CierreProyectoResponse(
    boolean success,
    String message,
    String errorBanner,
    LocalDateTime fechaCierre,
    BigDecimal avanceFinal,
    String archivoPdf,
    String downloadUrl
) {
    public static CierreProyectoResponse error(String banner) {
        return new CierreProyectoResponse(false, "No se pudo cerrar el proyecto", banner, null, null, null, null);
    }

    public static CierreProyectoResponse success(String message, LocalDateTime fecha, BigDecimal avance, String archivoPdf, String downloadUrl) {
        return new CierreProyectoResponse(true, message, null, fecha, avance, archivoPdf, downloadUrl);
    }
}
