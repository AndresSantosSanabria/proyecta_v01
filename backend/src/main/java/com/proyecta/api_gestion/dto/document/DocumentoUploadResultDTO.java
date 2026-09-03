package com.proyecta.api_gestion.dto.document;

public record DocumentoUploadResultDTO(
        Long id,
        String tipoDocumento,
        String nombreOriginal,
        String nombreAlmacenado,
        String mimeType,
        Long tamanoBytes,
        String tamanoFormateado,
        String urlDescarga,
        String fechaCarga,
        String usuario,
        String usuarioRol
) {
}
