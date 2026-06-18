package com.proyecta.api_gestion.dto.beneficioimpacto;

import java.time.LocalDateTime;

public record ProyectoBeneficioImpactoResponseDTO(
        Long id,
        String proyectoId,
        String proyectoNombre,
        String estado,
        LocalDateTime requeridoEn,
        String requeridoPor,
        LocalDateTime diligenciadoEn,
        String diligenciadoPor,
        LocalDateTime revisadoEn,
        String revisadoPor,
        String beneficioPrincipal,
        String impactoSocial,
        String observaciones,
        String snapshotJson,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        boolean requerido,
        boolean editable,
        boolean visibleParaGestor,
        boolean bloqueaCierre
) {
}
