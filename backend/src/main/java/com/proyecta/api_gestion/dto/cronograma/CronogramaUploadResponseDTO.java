package com.proyecta.api_gestion.dto.cronograma;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombreArchivo":"cronograma_maestro.xlsx","fechaCarga":"2026-07-15","descargaUrl":"/api/v1/proyectos/PROY-CUN-2026-008/cronograma/descarga"}
    """)
public record CronogramaUploadResponseDTO(
    String proyectoId,
    String nombreArchivo,
    LocalDate fechaCarga,
    String descargaUrl
) {}
