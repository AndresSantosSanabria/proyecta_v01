package com.proyecta.api_gestion.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(example = """
    {"id":101,"nombre":"Documento de alcance aprobado","fechaEntrega":"2026-07-10","faseNombre":"Planificación"}
    """)
public record EntregablePendienteDTO(
    Integer id,
    String nombre,
    LocalDate fechaEntrega,
    String faseNombre
) {}
