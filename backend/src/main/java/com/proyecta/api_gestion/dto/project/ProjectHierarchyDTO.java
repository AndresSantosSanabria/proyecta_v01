package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {
      "proyectoId": "PROY-CUN-2026-008",
      "nombreProyecto": "Modernización de Redes LAN",
      "fases": [
        {
          "id": 12,
          "numero": 1,
          "nombre": "Planificación",
          "descripcion": "Definición del alcance y cronograma",
          "ponderacion": 20.00,
          "avanceCalculado": 100.00,
          "hitos": []
        }
      ]
    }
    """)
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
