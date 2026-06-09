package com.proyecta.api_gestion.dto.report;

public record RiesgoVerificacionReporteDTO(
        String proyectoId,
        String nombreProyecto,
        String directorProyecto,
        String dependencia,
        boolean diligencioTratamiento
) {
}
