package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
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
    EstadoProyecto estado,
    BigDecimal avanceTotal,
    Boolean peti,
    String vigenciaPeti,
    EstrategiaPeti estrategiaPeti,
    Boolean tienePlanComunicaciones,
    PatrocinadorDTO patrocinador,
    List<EquipoTrabajoDTO> equipoTrabajo,
    FuragDTO furag,
    List<FaseResponseDTO> fases
) {}
