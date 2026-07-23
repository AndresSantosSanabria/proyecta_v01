package com.proyecta.api_gestion.dto.document;

import java.util.List;

public record DocumentoVersionHistorialResponseDTO(
        String proyectoId,
        String tipoDocumento,
        List<DocumentoProyectoVersionDTO> versiones
) {
}
