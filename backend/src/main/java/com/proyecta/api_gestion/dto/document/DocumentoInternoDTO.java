package com.proyecta.api_gestion.dto.document;

import java.util.List;

public record DocumentoInternoDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String fechaCreacion,
        String nombreOriginal,
        String mimeType,
        Long tamanoBytes,
        String tamanoFormateado,
        String creadoPor,
        String creadoEn
) {
    public record Listado(
            List<DocumentoInternoDTO> documentos,
            long total,
            int page,
            int size
    ) {
        public Listado(List<DocumentoInternoDTO> documentos, long total) {
            this(documentos, total, 0, documentos != null ? documentos.size() : 0);
        }
    }
}
