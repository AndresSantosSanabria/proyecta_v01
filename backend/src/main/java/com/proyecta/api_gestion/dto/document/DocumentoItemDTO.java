package com.proyecta.api_gestion.dto.document;

import com.proyecta.api_gestion.model.enums.TipoDocumento;
import java.time.LocalDate;

public record DocumentoItemDTO(
        TipoDocumento tipo,
        Boolean requerido,
        Boolean cargado,
        String nombreArchivo,
        LocalDate fechaCarga,
        LocalDate fechaLimiteActa,
        Integer diasRestantes,
        String descargaUrl
) {
}
