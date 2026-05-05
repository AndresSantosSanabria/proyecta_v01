package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.RespuestaFurag;
import jakarta.validation.constraints.NotNull;

public record FuragDTO(
    @NotNull RespuestaFurag infraestructuraDatos,
    @NotNull RespuestaFurag interoperabilidad,
    @NotNull RespuestaFurag digitalizacionAutomatizacion,
    @NotNull RespuestaFurag contratacionPublica,
    @NotNull RespuestaFurag serviciosNube,
    @NotNull RespuestaFurag sandbox,
    @NotNull RespuestaFurag tecnologiasEmergentes
) {}
