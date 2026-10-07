package com.proyecta.api_gestion.dto.advance;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {"projectId":"PROY-CUN-2026-008","projectName":"Modernización de Redes LAN","isPending":false,"isUploaded":true,"dueDate":"2026-07-20","daysUntilDue":5,"isOverdue":false,"periodo":"2026-07","fileName":"informe_avance_julio.xlsx","uploadedAt":"2026-07-15T10:30:00","uploadedBy":"luis.coordinador@proyecta.gov.co","estado":"VERIFICADO","observaciones":null,"verifiedBy":"ana.revision@proyecta.gov.co","returnedBy":null}
    """)
public record AdvanceReportStatusDTO(
        String projectId,
        String projectName,
        boolean isPending,
        boolean isUploaded,
        LocalDate dueDate,
        long daysUntilDue,
        boolean isOverdue,
        String periodo,
        String fileName,
        String uploadedAt,
        String uploadedBy,
        String estado,
        String observaciones,
        String verifiedBy,
        String returnedBy
) {}
