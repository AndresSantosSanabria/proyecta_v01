package com.proyecta.api_gestion.dto.proyecto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProyectoCompletionStatusDTO(
        String proyectoId,
        String estado,
        boolean requiereCompletitud,
        boolean primerIngresoRegistrado,
        boolean puedeCompletar,
        LocalDateTime primerIngresoDirectorAt,
        LocalDateTime completadoPorDirectorAt,
        String viabilidadEstado,
        String viabilidadObservaciones,
        boolean viabilidadAprobada,
        boolean documentosCargados,
        boolean documentosVerificados,
        boolean puedeCargarViabilidad,
        boolean puedeCompletarWizard,
        LocalDate fechaLimiteCompletar,
        boolean plazoVencido,
        boolean cierreForzoso,
        String mensaje
) {
}
