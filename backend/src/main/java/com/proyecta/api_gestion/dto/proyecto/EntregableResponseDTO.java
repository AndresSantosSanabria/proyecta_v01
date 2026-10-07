package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(example = """
    {"id":101,"nombre":"Documento de alcance aprobado","descripcion":"Alcance funcional y técnico del proyecto","ponderacion":5.00,"estado":"PENDIENTE","conforme":false,"fechaInicio":"2026-07-01","fechaLimite":"2026-07-20"}
    """)
public record EntregableResponseDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    String estado,
    Boolean conforme,
    LocalDate fechaInicio,
    LocalDate fechaLimite
) {}
