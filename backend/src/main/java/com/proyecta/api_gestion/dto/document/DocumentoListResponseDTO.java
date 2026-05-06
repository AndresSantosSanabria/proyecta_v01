package com.proyecta.api_gestion.dto.document;

import java.util.List;

public record DocumentoListResponseDTO(
        String proyectoId,
        List<DocumentoItemDTO> documentos
) {
}
