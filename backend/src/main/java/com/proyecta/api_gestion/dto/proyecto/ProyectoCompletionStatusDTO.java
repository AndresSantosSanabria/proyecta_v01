package com.proyecta.api_gestion.dto.proyecto;

import java.time.LocalDateTime;

public record ProyectoCompletionStatusDTO(
        String proyectoId,
        String estado,
        boolean requiereCompletitud,
        boolean primerIngresoRegistrado,
        boolean puedeCompletar,
        LocalDateTime primerIngresoDirectorAt,
        LocalDateTime completadoPorDirectorAt,
        String mensaje
) {
}
