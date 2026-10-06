package com.proyecta.api_gestion.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {
      "id": 45,
      "numero": 2,
      "nombre": "Levantamiento de información",
      "descripcion": "Recopilación de requerimientos con las dependencias",
      "ponderacion": 15.00,
      "avanceCalculado": 60.00,
      "entregables": []
    }
    """)
public class HitoHierarchyDTO extends PlanificacionHierarchyDTO {
    private List<EntregableHierarchyDTO> entregables;

    public HitoHierarchyDTO() {}

    public HitoHierarchyDTO(Integer id, Short numero, String nombre, String descripcion, BigDecimal ponderacion,
                            BigDecimal avanceCalculado, List<EntregableHierarchyDTO> entregables) {
        super(id, numero, nombre, descripcion, ponderacion, avanceCalculado);
        this.entregables = entregables;
    }

    // Getters and Setters
    public List<EntregableHierarchyDTO> getEntregables() { return entregables; }
    public void setEntregables(List<EntregableHierarchyDTO> entregables) { this.entregables = entregables; }
}
