package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.dto.document.DocumentoPreWizardRevisionDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(example = """
    {
      "proyectoId": "PROY-CUN-2026-008",
      "estado": "PENDIENTE_COMPLETAR",
      "requiereCompletitud": true,
      "primerIngresoRegistrado": true,
      "puedeCompletar": true,
      "primerIngresoDirectorAt": "2026-07-10T08:15:00",
      "completadoPorDirectorAt": null,
      "viabilidadEstado": "PENDIENTE",
      "viabilidadObservaciones": null,
      "viabilidadAprobada": false,
      "documentosCargados": true,
      "documentosVerificados": false,
      "puedeCargarViabilidad": true,
      "puedeCompletarWizard": false,
      "fechaLimiteCompletar": "2026-07-31",
      "plazoVencido": false,
      "cierreForzoso": false,
      "mensaje": "Complete la información pendiente del wizard",
      "documentosPreWizard": []
    }
    """)
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
