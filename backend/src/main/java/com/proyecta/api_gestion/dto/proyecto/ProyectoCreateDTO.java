package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record ProyectoCreateDTO(
    @NotBlank String nombre,
    @NotBlank String dependencia,
    @NotBlank String director,
    @NotBlank @Email String correoDirector,
    @NotBlank String objetivoGeneral,
    List<String> objetivosEspecificos,
    @NotNull LocalDate fechaInicio,
    @NotNull PatrocinadorDTO patrocinador,
    List<EquipoTrabajoDTO> equipoTrabajo,
    @NotEmpty List<FaseDTO> fases,
    @NotNull Boolean peti,
    String vigenciaPeti,
    EstrategiaPeti estrategiaPeti,
    @NotNull Boolean tienePlanComunicaciones,
    @NotNull FuragDTO furag
) {}
