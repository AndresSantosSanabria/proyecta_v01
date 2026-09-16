package com.proyecta.api_gestion.dto.risk;

import java.time.LocalDateTime;

public record RiesgoTratamientoAdjuntoDTO(
        Long id,
        String nombreOriginal,
        String nombreAlmacenado,
        String mimeType,
        Long tamanoBytes,
        String urlDescarga,
        LocalDateTime fechaCarga
) {}
