package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {"id":12,"nombre":"Planificación","descripcion":"Definición del alcance y cronograma","ponderacion":20.00,"avanceCalculado":95.00,"hitos":[]}
    """)
public record FaseResponseDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    BigDecimal avanceCalculado,
    List<HitoResponseDTO> hitos
) {}
