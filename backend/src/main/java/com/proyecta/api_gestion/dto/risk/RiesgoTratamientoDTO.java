package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(example = """
    {"id":701,"iteracion":1,"comentario":"Se definió plan de contingencia aprobado por el comité","fechaCreacion":"2026-07-15T10:30:00","adjuntos":[]}
    """)
public record RiesgoTratamientoDTO(
        Long id,
        Integer iteracion,
        String comentario,
        LocalDateTime fechaCreacion,
        List<RiesgoTratamientoAdjuntoDTO> adjuntos
) {}
