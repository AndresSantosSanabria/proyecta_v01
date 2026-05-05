package com.proyecta.api_gestion.dto.cronograma;

import java.util.List;

public record FaseGanttDTO(
    Integer faseId,
    String nombre,
    List<HitoGanttDTO> hitos
) {}
