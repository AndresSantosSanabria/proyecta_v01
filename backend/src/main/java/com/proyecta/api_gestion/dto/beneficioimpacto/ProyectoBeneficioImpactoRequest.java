package com.proyecta.api_gestion.dto.beneficioimpacto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProyectoBeneficioImpactoRequest(
        @NotBlank @Size(max = 4000) String beneficiosValorPublico,
        @NotBlank @Size(max = 4000) String impactosValorPublico,
        @Size(max = 4000) String observaciones
) {
}
