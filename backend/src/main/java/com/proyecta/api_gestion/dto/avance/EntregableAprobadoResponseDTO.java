package com.proyecta.api_gestion.dto.avance;

import java.time.LocalDate;

public record EntregableAprobadoResponseDTO(
    Integer entregableId,
    String estado,
    LocalDate fechaEntrega,
    String evidenciaUrl,
    ProyectoAvanceResponseDTO avanceActualizado
) {}
