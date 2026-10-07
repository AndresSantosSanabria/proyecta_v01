package com.proyecta.api_gestion.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(example = """
    {"totalRegistros":154,"totalExitosos":141,"totalErrores":13,"porAccion":{"LOGIN":40,"CREAR_PROYECTO":25,"ACTUALIZAR_RIESGO":18},"porCodigoEstado":{"200":120,"201":21,"401":5,"500":8}}
    """)
public record AuditLogStatsDTO(
        long totalRegistros,
        long totalExitosos,
        long totalErrores,
        Map<String, Long> porAccion,
        Map<Integer, Long> porCodigoEstado
) {
}