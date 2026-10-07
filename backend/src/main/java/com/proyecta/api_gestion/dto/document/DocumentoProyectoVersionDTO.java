package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":7712,"tipoDocumento":"VIABILIZACION","numeroVersion":2,"nombreArchivo":"informe_viabilidad_v2.pdf","mimeType":"application/pdf","tamanoBytes":524288,"tamanoFormateado":"512 KB","observacion":"Corrección de observaciones del revisor","estado":"ACTUAL","subidoPor":"ana.gestion@proyecta.gov.co","subidoRol":"GESTOR","subidoEn":"2026-07-15T10:30:00","actual":true}
    """)
public record DocumentoProyectoVersionDTO(
        Long id,
        String tipoDocumento,
        Integer numeroVersion,
        String nombreArchivo,
        String mimeType,
        Long tamanoBytes,
        String tamanoFormateado,
        String observacion,
        String estado,
        String subidoPor,
        String subidoRol,
        String subidoEn,
        Boolean actual
) {
}
