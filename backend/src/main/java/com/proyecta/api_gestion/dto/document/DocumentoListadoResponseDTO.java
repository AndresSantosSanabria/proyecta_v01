package com.proyecta.api_gestion.dto.document;

import com.proyecta.api_gestion.model.enums.TipoDocumento;
import java.util.List;

public record DocumentoListadoResponseDTO(
        String proyectoId,
        List<DocumentoDetailDTO> documentos
) {
}
