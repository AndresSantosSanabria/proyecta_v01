package com.proyecta.api_gestion.dto.timeline;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Respuesta estructurada para el cronograma del proyecto")
public class TimelineResponseDTO {

    @Schema(description = "Lista de fases con sus hitos anidados")
    private List<TimelineItemDTO> fases;

    public TimelineResponseDTO() {}

    public TimelineResponseDTO(List<TimelineItemDTO> fases) {
        this.fases = fases;
    }

    public List<TimelineItemDTO> getFases() {
        return fases;
    }

    public void setFases(List<TimelineItemDTO> fases) {
        this.fases = fases;
    }
}
