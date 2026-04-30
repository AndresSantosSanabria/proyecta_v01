package com.proyecta.api_gestion.dto.proyecto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO para actualizar un proyecto existente.
 * Todos los campos son opcionales - solo se actualizan los enviados.
 */
@Schema(description = "Solicitud para actualizar un proyecto existente")
public class ProyectoUpdateDTO {

    @Schema(description = "Nombre del proyecto", example = "Sistema de Gestión de Proyectos")
    @Size(max = 300, message = "El nombre no puede exceder 300 caracteres")
    private String nombre;

    @Schema(description = "Dependencia o unidad responsable", example = "Dirección de TI")
    @Size(max = 200, message = "La dependencia no puede exceder 200 caracteres")
    private String dependencia;

    @Schema(description = "Objetivo general del proyecto")
    @Size(max = 2000, message = "El objetivo general no puede exceder 2000 caracteres")
    private String objetivoGeneral;

    @Schema(description = "Indica si el proyecto es PETI", example = "false")
    private Boolean esPeti;

    @Schema(description = "Estrategia PETI asociada", example = "Transformación Digital")
    @Size(max = 80, message = "La estrategia PETI no puede exceder 80 caracteres")
    private String estrategiaPeti;

    @Schema(description = "Vigencia PETI", example = "2026")
    @Size(max = 20, message = "La vigencia PETI no puede exceder 20 caracteres")
    private String vigenciaPeti;

    @Schema(description = "Fecha de inicio del proyecto", example = "2026-01-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    @Schema(description = "Fecha estimada de cierre", example = "2026-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaCierre;

    @Schema(description = "Estado del proyecto", example = "activo")
    private EstadoProyecto estado;

    @Schema(description = "Indica si el proyecto está cerrado", example = "false")
    private Boolean cerrado;

    @Schema(description = "ID del gestor del proyecto", example = "123")
    private Integer gestorId;

    @Schema(description = "ID del director del proyecto", example = "456")
    private Integer directorId;

    @Schema(description = "ID del patrocinador", example = "789")
    private Integer patrocinadorId;

    // Getters and Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDependencia() { return dependencia; }
    public void setDependencia(String dependencia) { this.dependencia = dependencia; }
    public String getObjetivoGeneral() { return objetivoGeneral; }
    public void setObjetivoGeneral(String objetivoGeneral) { this.objetivoGeneral = objetivoGeneral; }
    public Boolean getEsPeti() { return esPeti; }
    public void setEsPeti(Boolean esPeti) { this.esPeti = esPeti; }
    public String getEstrategiaPeti() { return estrategiaPeti; }
    public void setEstrategiaPeti(String estrategiaPeti) { this.estrategiaPeti = estrategiaPeti; }
    public String getVigenciaPeti() { return vigenciaPeti; }
    public void setVigenciaPeti(String vigenciaPeti) { this.vigenciaPeti = vigenciaPeti; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDate fechaCierre) { this.fechaCierre = fechaCierre; }
    public EstadoProyecto getEstado() { return estado; }
    public void setEstado(EstadoProyecto estado) { this.estado = estado; }
    public Boolean getCerrado() { return cerrado; }
    public void setCerrado(Boolean cerrado) { this.cerrado = cerrado; }
    public Integer getGestorId() { return gestorId; }
    public void setGestorId(Integer gestorId) { this.gestorId = gestorId; }
    public Integer getDirectorId() { return directorId; }
    public void setDirectorId(Integer directorId) { this.directorId = directorId; }
    public Integer getPatrocinadorId() { return patrocinadorId; }
    public void setPatrocinadorId(Integer patrocinadorId) { this.patrocinadorId = patrocinadorId; }
}
