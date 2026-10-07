package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":4501,"entregableId":101,"descripcionAnterior":"Documento de alcance preliminar","descripcionNueva":"Documento de alcance aprobado por el comité","justificacion":"Ajuste solicitado en la reunión de seguimiento","archivoPdf":"justificacion_cambio_descripcion.pdf","nombreOriginal":"justificacion.pdf","usuario":"luis.coordinador@proyecta.gov.co","usuarioRol":"COORDINADOR","creadoEn":"2026-07-15T10:30:00"}
    """)
public record CambioDescripcionResponse(
    Long id,
    Integer entregableId,
    String descripcionAnterior,
    String descripcionNueva,
    String justificacion,
    String archivoPdf,
    String nombreOriginal,
    String usuario,
    String usuarioRol,
    LocalDateTime creadoEn
) {}