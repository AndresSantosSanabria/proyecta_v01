package com.proyecta.api_gestion.dto.avance;

public record DocumentoArchivoDTO(
        String archivoStorage,
        String nombreArchivo,
        String mimeType
) {
}
