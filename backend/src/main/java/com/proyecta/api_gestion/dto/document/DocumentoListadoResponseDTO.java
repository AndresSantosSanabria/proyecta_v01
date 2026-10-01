package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","documentos":[{"id":7701,"tipoDocumento":"VIABILIZACION","nombreOriginal":"informe_viabilidad.pdf","mimeType":"application/pdf","tamanoBytes":524288,"tamanoFormateado":"512 KB","urlDescarga":"/api/v1/documentos/7701/descarga","fechaCarga":"2026-07-15T10:30:00","usuario":"ana.gestion@proyecta.gov.co","usuarioRol":"GESTOR"}]}
    """)
public record DocumentoListadoResponseDTO(
        String proyectoId,
        List<DocumentoDetailDTO> documentos
) {
}
