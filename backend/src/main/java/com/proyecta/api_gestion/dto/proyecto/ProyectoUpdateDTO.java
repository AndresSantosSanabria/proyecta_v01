package com.proyecta.api_gestion.dto.proyecto;

import java.time.LocalDate;
import java.util.List;

public record ProyectoUpdateDTO(
    String nombre,
    String dependencia,
    String director,
    String correoDirector,
    String objetivoGeneral,
    List<String> objetivosEspecificos,
    LocalDate fechaInicio,
    PatrocinadorDTO patrocinador,
    List<EquipoTrabajoDTO> equipoTrabajo,
    List<StakeholderDTO> stakeholders,
    List<FaseDTO> fases,
    Boolean peti,
    String vigenciaPeti,
    String estrategiaPeti,
    Boolean tienePlanComunicaciones,
    FuragDTO furag
) {}
