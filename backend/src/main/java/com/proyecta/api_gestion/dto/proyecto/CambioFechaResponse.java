package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":4401,"entregableId":101,"fechaAnterior":"2026-07-20","fechaNueva":"2026-07-27","justificacion":"Reprogramación por indisponibilidad del proveedor","archivoPdf":"justificacion_cambio_fecha.pdf","nombreOriginal":"justificacion.pdf","usuario":"luis.coordinador@proyecta.gov.co","usuarioRol":"COORDINADOR","creadoEn":"2026-07-15T10:30:00"}
    """)
public record CambioFechaResponse(
    Long id,
    Integer entregableId,
    LocalDate fechaAnterior,
    LocalDate fechaNueva,
    String justificacion,
    String archivoPdf,
    String nombreOriginal,
    String usuario,
    String usuarioRol,
    LocalDateTime creadoEn
) {}
