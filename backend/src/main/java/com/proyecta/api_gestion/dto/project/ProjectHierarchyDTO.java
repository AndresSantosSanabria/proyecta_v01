package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectHierarchyDTO {
    private String proyectoId;
    private String nombreProyecto;
    private List<FaseHierarchyDTO> fases;

    public ProjectHierarchyDTO() {}

    public ProjectHierarchyDTO(String proyectoId, String nombreProyecto, List<FaseHierarchyDTO> fases) {
        this.proyectoId = proyectoId;
        this.nombreProyecto = nombreProyecto;
        this.fases = fases;
    }

    // Getters and Setters
    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }
    public String getNombreProyecto() { return nombreProyecto; }
    public void setNombreProyecto(String nombreProyecto) { this.nombreProyecto = nombreProyecto; }
    public List<FaseHierarchyDTO> getFases() { return fases; }
    public void setFases(List<FaseHierarchyDTO> fases) { this.fases = fases; }
}
