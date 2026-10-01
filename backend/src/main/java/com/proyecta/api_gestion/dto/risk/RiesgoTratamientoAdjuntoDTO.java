package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":801,"nombreOriginal":"acta_comite.pdf","nombreAlmacenado":"3b7d5e21-9c0f-4a2b-8d64-bbbbbbbbbbbb.pdf","mimeType":"application/pdf","tamanoBytes":184320,"urlDescarga":"/api/v1/riesgos/34/tratamientos/701/adjuntos/801/descarga","fechaCarga":"2026-07-15T10:30:00"}
    """)
public record RiesgoTratamientoAdjuntoDTO(
        Long id,
        String nombreOriginal,
        String nombreAlmacenado,
        String mimeType,
        Long tamanoBytes,
        String urlDescarga,
        LocalDateTime fechaCarga
) {}
