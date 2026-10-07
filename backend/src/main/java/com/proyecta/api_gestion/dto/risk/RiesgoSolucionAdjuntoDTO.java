package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":501,"nombreOriginal":"plan_contingencia.pdf","nombreAlmacenado":"2f9c1a7e-3d4b-4c8a-9f10-aaaaaaaaaaaa.pdf","mimeType":"application/pdf","tamanoBytes":245760,"urlDescarga":"/api/v1/riesgos/34/soluciones/501/descarga","fechaCarga":"2026-07-15T10:30:00"}
    """)
public record RiesgoSolucionAdjuntoDTO(
        Long id,
        String nombreOriginal,
        String nombreAlmacenado,
        String mimeType,
        Long tamanoBytes,
        String urlDescarga,
        LocalDateTime fechaCarga
) {}
