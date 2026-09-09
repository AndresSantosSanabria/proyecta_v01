package com.proyecta.api_gestion.dto.audit;

import java.util.List;

public record AuditLogPageResponse(
        List<SystemAuditLogDTO> entries,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}