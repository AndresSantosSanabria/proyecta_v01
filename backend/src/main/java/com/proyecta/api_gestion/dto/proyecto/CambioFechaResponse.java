package com.proyecta.api_gestion.dto.proyecto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
