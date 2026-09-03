package com.proyecta.api_gestion.dto.proyecto;

import java.util.List;
import java.util.Map;

public record CompletitudBorradorDTO(
        Integer faseActual,
        Map<Integer, Boolean> fasesCompletadas,
        DatosFase1 datosFase1,
        DatosFase2 datosFase2,
        DatosFase3 datosFase3,
        DatosFase4 datosFase4,
        DatosFase5 datosFase5,
        DatosFase6 datosFase6,
        DatosFase7 datosFase7,
        String ultimoGuardado
) {
    public CompletitudBorradorDTO {
        if (fasesCompletadas == null) {
            fasesCompletadas = Map.of();
        }
        if (faseActual == null) {
            faseActual = 1;
        }
    }

    public record DatosFase1(
            String dependencia,
            String fechaInicio,
            String presupuestoEstimado,
            String alcance,
            List<String> objetivosEspecificos
    ) {}

    public record DatosFase2(
            PatrocinadorDTO patrocinador,
            List<EquipoTrabajoDTO> equipoTrabajo,
            List<StakeholderDTO> stakeholders
    ) {}

    public record DatosFase3(
            List<FaseDTO> fases
    ) {}

    public record DatosFase4(
            Boolean peti,
            String vigenciaPeti,
            String estrategiaPeti,
            Boolean tienePlanComunicaciones
    ) {}

    public record DatosFase5(
            FuragDTO furag
    ) {}

    public record DatosFase6(
            List<RiesgoCompletitudDTO> riesgos
    ) {}

    public record DatosFase7(
            Map<String, String> documentos
    ) {}
}
