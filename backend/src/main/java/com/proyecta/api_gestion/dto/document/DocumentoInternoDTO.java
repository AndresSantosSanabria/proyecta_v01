package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"id":8801,"codigo":"DOC-INT-2026-014","nombre":"Acta de comité directivo","descripcion":"Acta de la reunión del comité directivo del proyecto","fechaCreacion":"2026-07-15","nombreOriginal":"acta_comite_directivo.pdf","mimeType":"application/pdf","tamanoBytes":314572,"tamanoFormateado":"307 KB","creadoPor":"Ana Gestión","creadoPorId":42,"creadoEn":"2026-07-15T10:30:00"}
    """)
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
        Long creadoPorId,
        String creadoEn
) {
    @Schema(example = """
        {"documentos":[{"id":8801,"codigo":"DOC-INT-2026-014","nombre":"Acta de comité directivo","descripcion":"Acta de la reunión del comité directivo del proyecto","fechaCreacion":"2026-07-15","nombreOriginal":"acta_comite_directivo.pdf","mimeType":"application/pdf","tamanoBytes":314572,"tamanoFormateado":"307 KB","creadoPor":"Ana Gestión","creadoPorId":42,"creadoEn":"2026-07-15T10:30:00"}],"total":1,"page":0,"size":10}
        """)
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
