package com.proyecta.api_gestion.dto.document;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentoPreWizardRevisionDTO(
        String tipoDocumento,
        String estado,
        String observacion,
        String revisadoPor,
        LocalDateTime revisadoEn
) {
    public record Listado(String proyectoId, List<DocumentoPreWizardRevisionDTO> documentos) {
    }
}
