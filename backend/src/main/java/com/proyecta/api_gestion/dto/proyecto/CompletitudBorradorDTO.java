package com.proyecta.api_gestion.dto.proyecto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(example = """
    {
      "faseActual": 1,
      "fasesCompletadas": {"1": true, "2": false, "3": false, "4": false, "5": false, "6": false, "7": false},
      "datosFase1": {"dependencia": "Secretaría de Transformación Digital", "fechaInicio": "2026-03-02", "presupuestoEstimado": "1250000000", "alcance": "Actualización de equipos de red en 8 sedes", "objetivosEspecificos": ["Actualizar switches core"]},
      "datosFase2": {"patrocinador": {"nombre": "Carlos Ramírez", "cargo": "Director de TIC", "procesoSigc": null, "procedimientoSigc": null}, "equipoTrabajo": [], "stakeholders": []},
      "datosFase3": {"fases": []},
      "datosFase4": {"peti": false, "vigenciaPeti": null, "estrategiaPeti": null, "tienePlanComunicaciones": false},
      "datosFase5": {"furag": null},
      "datosFase6": {"riesgos": []},
      "datosFase7": {"documentos": {}},
      "ultimoGuardado": "2026-07-15T10:30:00"
    }
    """)
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
