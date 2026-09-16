package com.proyecta.api_gestion.dto.risk;

import java.time.LocalDateTime;
import java.util.List;

public record RiesgoTratamientoDTO(
        Long id,
        Integer iteracion,
        String comentario,
        LocalDateTime fechaCreacion,
        List<RiesgoTratamientoAdjuntoDTO> adjuntos
) {}
