package com.proyecta.api_gestion.dto.avance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectEvidenceDTO(
        String id,
        String categoria,
        String nombre,
        String nombreArchivo,
        String evidenciaUrl,
        LocalDate fechaRegistro,
        LocalDate fechaEntrega,
        LocalDate fechaLimite,
        String estado,
        String estadoCodigo,
        String usuario,
        String tipo,
        String tipoDocumento,
        String faseNombre,
        String hitoNombre,
        String entregableNombre,
        String descripcion,
        String observaciones,
        Integer entregableId,
        Long cambioId,
        Long riesgoId,
        Integer riesgoSolucionId,
        String archivoPdf,
        String fechaAnterior,
        String fechaNueva,
        String justificacion,
        Long tamanoBytes,
        String mimeType,
        String descripcionAnterior,
        String descripcionNueva,
        Integer solucionesCount
) {}