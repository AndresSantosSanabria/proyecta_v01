package com.proyecta.api_gestion.dto.advance;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
