package com.proyecta.api_gestion.dto.audit;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"entries":[],"page":0,"size":20,"totalElements":154,"totalPages":8}
    """)
public record AuditLogPageResponse(
        List<SystemAuditLogDTO> entries,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}