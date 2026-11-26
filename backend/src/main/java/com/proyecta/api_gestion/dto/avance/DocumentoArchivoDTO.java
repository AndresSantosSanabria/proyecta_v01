package com.proyecta.api_gestion.dto.avance;

/**
 * Referencia al archivo fisico de una version documental de evidencia.
 */
public record DocumentoArchivoDTO(
        String archivoStorage,
        String nombreArchivo,
        String mimeType
) {
}
