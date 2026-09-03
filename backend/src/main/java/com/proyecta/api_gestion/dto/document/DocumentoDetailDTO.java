package com.proyecta.api_gestion.dto.document;

public record DocumentoDetailDTO(
        Long id,
        String tipoDocumento,
        String nombreOriginal,
        String mimeType,
        Long tamanoBytes,
        String tamanoFormateado,
        String urlDescarga,
        String fechaCarga,
        String usuario,
        String usuarioRol
) {
}
