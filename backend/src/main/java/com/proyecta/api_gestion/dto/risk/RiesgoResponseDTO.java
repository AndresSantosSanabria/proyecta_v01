package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.model.enums.Probabilidad;

public record RiesgoResponseDTO(
    Integer id,
    String codigo,
    String descripcion,
    Probabilidad probabilidad,
    Impacto impacto,
    NivelRiesgo nivel,
    String tratamiento,
    EstadoRiesgo estado
) {}
