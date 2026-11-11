package com.proyecta.api_gestion.dto.report;

public record PlanComunicacionesProyectoDTO(
        String id,
        String nombre,
        Boolean peti,
        Boolean petiDefinido,
        Boolean tienePlanComunicaciones
) {}
