package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":"PROY-CUN-2026-008","codigo":"PROY-CUN-2026-008","nombre":"Modernización de Redes LAN","estado":"PENDIENTE_COMPLETAR","mensaje":"Proyecto creado correctamente"}
    """)
public record ProyectoCreatedDTO(
    String id,
    String codigo,
    String nombre,
    EstadoProyecto estado,
    String mensaje
) {}
