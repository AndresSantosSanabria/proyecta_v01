package com.proyecta.api_gestion.dto.proyecto;

import java.time.LocalDateTime;

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