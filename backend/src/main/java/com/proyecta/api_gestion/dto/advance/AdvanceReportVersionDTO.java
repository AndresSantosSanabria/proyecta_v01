package com.proyecta.api_gestion.dto.advance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {"numeroVersion":2,"fileName":"informe_avance_julio_v2.xlsx","fileSize":184320,"mimeType":"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","estado":"VERIFICADO","observacion":"Se corrigió el consolidado de entregables","subidoPor":"luis.coordinador@proyecta.gov.co","subidoRol":"COORDINADOR","subidoEn":"2026-07-15T10:30:00"}
    """)
public record AdvanceReportVersionDTO(
        Integer numeroVersion,
        String fileName,
        Long fileSize,
        String mimeType,
        String estado,
        String observacion,
        String subidoPor,
        String subidoRol,
        LocalDateTime subidoEn
) {}
