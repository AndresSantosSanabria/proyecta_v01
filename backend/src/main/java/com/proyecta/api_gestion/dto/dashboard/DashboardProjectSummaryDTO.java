package com.proyecta.api_gestion.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

public class DashboardProjectSummaryDTO {

    @Schema(description = "Codigo de los proyectos registrados", example = "IS-PROY-005")
    private String codigo;

    @Schema(description = "Nombre del proyecto registrado", example = "Modernización de Redes LAN")
    private String nombreProyecto;

    @Schema(description = "Nombre de la dependencia solicitante del proyecto", example = "Secretaría de Transformación Digital")
    private String nombreDependencia;

    @Schema(description = "Avance calculado del proyecto", example = "85")
    private Integer avance;

    @Schema(description = "Estado en el que se encuentra el proyecto", example = "Con retrasos")
    private String estado;

    @Schema(description = "Contador de la cantidad de entregables atrasados en el proyecto", example = "2")
    private Integer entregablesAtrasados;

    public DashboardProjectSummaryDTO() {}

    public DashboardProjectSummaryDTO(String codigo, String nombreProyecto, String nombreDependencia,
                                      Integer avance, String estado, Integer entregablesAtrasados) {
        this.codigo = codigo;
        this.nombreProyecto = nombreProyecto;
        this.nombreDependencia = nombreDependencia;
        this.avance = avance;
        this.estado = estado;
        this.entregablesAtrasados = entregablesAtrasados;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }

    public String getNombreDependencia() { return nombreDependencia; }
    public void setNombreDependencia(String nombreDependencia) { this.nombreDependencia = nombreDependencia; }

    public Integer getAvance() { return avance != null ? avance : 0; }
    public void setAvance(Integer avance) { this.avance = avance; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getEntregablesAtrasados() { return entregablesAtrasados != null ? entregablesAtrasados : 0; }
    public void setEntregablesAtrasados(Integer entregablesAtrasados) { this.entregablesAtrasados = entregablesAtrasados; }
}
