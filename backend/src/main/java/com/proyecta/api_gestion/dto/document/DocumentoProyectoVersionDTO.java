package com.proyecta.api_gestion.dto.document;

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
