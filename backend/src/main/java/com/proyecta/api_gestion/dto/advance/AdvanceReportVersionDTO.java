package com.proyecta.api_gestion.dto.advance;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
