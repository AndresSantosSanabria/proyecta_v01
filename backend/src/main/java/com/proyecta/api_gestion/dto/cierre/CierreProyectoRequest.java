package com.proyecta.api_gestion.dto.cierre;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CierreProyectoRequest(
    @NotBlank(message = "El resumen ejecutivo es obligatorio.")
    @Size(min = 100, message = "El resumen ejecutivo debe tener al menos 100 caracteres.")
    String resumenEjecutivo,

    @NotBlank(message = "Las lecciones positivas son obligatorias.")
    @Size(max = 2000, message = "Las lecciones positivas no pueden superar 2000 caracteres.")
    String leccionesPositivas,

    @NotBlank(message = "Las lecciones a mejorar son obligatorias.")
    @Size(max = 2000, message = "Las lecciones a mejorar no pueden superar 2000 caracteres.")
    String leccionesMejorar,

    @NotBlank(message = "Las recomendaciones son obligatorias.")
    @Size(max = 2000, message = "Las recomendaciones no pueden superar 2000 caracteres.")
    String recomendaciones,

    @NotBlank(message = "La actividad de transferencia es obligatoria.")
    @Size(max = 1500, message = "La actividad de transferencia no puede superar 1500 caracteres.")
    String transferenciaActividad,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate transferenciaFecha,

    @NotBlank(message = "La ubicación de la evidencia es obligatoria.")
    @Size(max = 500, message = "La ubicación de la evidencia no puede superar 500 caracteres.")
    String transferenciaUbicacionEvidencia,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate fechaCierre
) {}
