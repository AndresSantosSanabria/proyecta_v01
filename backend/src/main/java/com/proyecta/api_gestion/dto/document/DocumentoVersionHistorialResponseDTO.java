package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","tipoDocumento":"VIABILIZACION","versiones":[{"id":7712,"tipoDocumento":"VIABILIZACION","numeroVersion":2,"nombreArchivo":"informe_viabilidad_v2.pdf","mimeType":"application/pdf","tamanoBytes":524288,"tamanoFormateado":"512 KB","observacion":"Corrección de observaciones","estado":"ACTUAL","subidoPor":"ana.gestion@proyecta.gov.co","subidoRol":"GESTOR","subidoEn":"2026-07-15T10:30:00","actual":true}]}
    """)
public record DocumentoVersionHistorialResponseDTO(
        String proyectoId,
        String tipoDocumento,
        List<DocumentoProyectoVersionDTO> versiones
) {
}
