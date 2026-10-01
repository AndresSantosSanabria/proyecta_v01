package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(example = """
    {"tipoDocumento":"VIABILIZACION","estado":"APROBADO","observacion":null,"revisadoPor":"ana.revision@proyecta.gov.co","revisadoEn":"2026-07-15T10:30:00"}
    """)
public record DocumentoPreWizardRevisionDTO(
        String tipoDocumento,
        String estado,
        String observacion,
        String revisadoPor,
        LocalDateTime revisadoEn
) {
    @Schema(example = """
        {"proyectoId":"PROY-CUN-2026-008","documentos":[{"tipoDocumento":"VIABILIZACION","estado":"APROBADO","observacion":null,"revisadoPor":"ana.revision@proyecta.gov.co","revisadoEn":"2026-07-15T10:30:00"},{"tipoDocumento":"ACTA_CONSTITUCION","estado":"PENDIENTE","observacion":null,"revisadoPor":"ana.revision@proyecta.gov.co","revisadoEn":"2026-07-16T09:15:00"}]}
        """)
    public record Listado(String proyectoId, List<DocumentoPreWizardRevisionDTO> documentos) {
    }
}
