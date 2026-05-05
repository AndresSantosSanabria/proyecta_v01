package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;

public record ProyectoCreatedDTO(
    String id,
    String codigo,
    String nombre,
    EstadoProyecto estado,
    String mensaje
) {}
