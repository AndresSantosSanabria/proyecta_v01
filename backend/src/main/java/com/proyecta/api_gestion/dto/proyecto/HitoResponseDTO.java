package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {"id":45,"nombre":"Levantamiento de información","descripcion":"Recopilación de requerimientos con las dependencias","ponderacion":15.00,"avanceCalculado":75.00,"entregables":[]}
    """)
public record HitoResponseDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    BigDecimal avanceCalculado,
    List<EntregableResponseDTO> entregables
) {}
