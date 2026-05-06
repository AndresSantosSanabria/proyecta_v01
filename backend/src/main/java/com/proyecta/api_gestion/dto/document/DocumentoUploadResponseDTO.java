package com.proyecta.api_gestion.dto.document;

import com.proyecta.api_gestion.model.enums.TipoDocumento;
import java.time.LocalDate;

public record DocumentoUploadResponseDTO(
        TipoDocumento tipo,
        String nombreArchivo,
        LocalDate fechaCarga,
        String descargaUrl
) {
}
