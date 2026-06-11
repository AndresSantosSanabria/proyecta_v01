package com.proyecta.api_gestion.dto.avance;

import java.time.LocalDateTime;

public record DocumentoObservacionDTO(
        Long id,
        Integer entregableId,
        Long versionId,
        Integer numeroVersion,
        String observacion,
        String estado,
        String creadaPor,
        String creadaRol,
        LocalDateTime creadaEn,
        String subsanadaPor,
        LocalDateTime subsanadaEn,
        String comentarioSubsanacion,
        String cerradaPor,
        LocalDateTime cerradaEn
) {
}
