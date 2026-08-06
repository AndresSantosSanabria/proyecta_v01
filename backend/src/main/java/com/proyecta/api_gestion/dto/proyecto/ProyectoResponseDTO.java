package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProyectoResponseDTO(
    String id,
    String codigo,
    String nombre,
    String dependencia,
    String director,
    String correoDirector,
    String objetivoGeneral,
    List<String> objetivosEspecificos,
    LocalDate fechaInicio,
    String estado,
    BigDecimal avanceTotal,
    BigDecimal presupuestoEstimado,
    String alcanceDetallado,
    Boolean peti,
    String vigenciaPeti,
    String estrategiaPeti,
    Boolean tienePlanComunicaciones,
    PatrocinadorDTO patrocinador,
    List<EquipoTrabajoDTO> equipoTrabajo,
    FuragDTO furag,
    List<FaseResponseDTO> fases
) {}
