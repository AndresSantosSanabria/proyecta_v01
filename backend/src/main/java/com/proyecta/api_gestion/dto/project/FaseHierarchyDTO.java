package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {
      "id": 12,
      "numero": 1,
      "nombre": "Planificación",
      "descripcion": "Definición del alcance y cronograma",
      "ponderacion": 20.00,
      "avanceCalculado": 75.50,
      "hitos": []
    }
    """)
public class FaseHierarchyDTO extends PlanificacionHierarchyDTO {
    private List<HitoHierarchyDTO> hitos;

    public FaseHierarchyDTO() {}

    public FaseHierarchyDTO(Integer id, Short numero, String nombre, String descripcion, BigDecimal ponderacion,
                            BigDecimal avanceCalculado, List<HitoHierarchyDTO> hitos) {
        super(id, numero, nombre, descripcion, ponderacion, avanceCalculado);
        this.hitos = hitos;
    }

    // Getters and Setters
    public List<HitoHierarchyDTO> getHitos() { return hitos; }
    public void setHitos(List<HitoHierarchyDTO> hitos) { this.hitos = hitos; }
}
