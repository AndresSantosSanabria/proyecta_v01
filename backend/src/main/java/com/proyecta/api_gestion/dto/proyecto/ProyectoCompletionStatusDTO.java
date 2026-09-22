package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.dto.document.DocumentoPreWizardRevisionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        String mensaje,
        List<DocumentoPreWizardRevisionDTO> documentosPreWizard
) {
}
