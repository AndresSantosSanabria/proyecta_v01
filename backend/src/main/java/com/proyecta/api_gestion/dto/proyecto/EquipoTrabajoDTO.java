package com.proyecta.api_gestion.dto.proyecto;

public record EquipoTrabajoDTO(
    String nombre,
    String cargo,
    String rol,
    String dependencia,
    String telefono,
    String correo
) {}
