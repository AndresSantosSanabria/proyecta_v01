package com.proyecta.api_gestion.dto.avance;

import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableConformidadResponseDTO(
    Integer entregableId,
    EstadoEntregable estado,
    LocalDate fechaEntrega,
    String evidenciaUrl,
    BigDecimal avanceTotalActualizado
) {}
