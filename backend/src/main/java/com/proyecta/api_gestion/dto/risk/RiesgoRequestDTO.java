package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RiesgoRequestDTO(
    @NotBlank(message = "La descripción es obligatoria")
    String descripcion,
    
    @NotNull(message = "La probabilidad es obligatoria")
    Probabilidad probabilidad,
    
    @NotNull(message = "El impacto es obligatorio")
    Impacto impacto,
    
    @NotBlank(message = "El plan de tratamiento es obligatorio")
    String tratamiento,
    
    @NotNull(message = "El estado es obligatorio")
    EstadoRiesgo estado
) {}
