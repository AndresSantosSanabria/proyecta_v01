package com.proyecta.api_gestion.dto.cronograma;

import java.time.LocalDate;

public record CronogramaUploadResponseDTO(
    String proyectoId,
    String nombreArchivo,
    LocalDate fechaCarga,
    String descargaUrl
) {}
