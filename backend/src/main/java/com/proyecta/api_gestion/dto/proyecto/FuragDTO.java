package com.proyecta.api_gestion.dto.proyecto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecta.api_gestion.dto.config.FuragPreguntaRespuestaDTO;
import com.proyecta.api_gestion.model.enums.RespuestaFurag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FuragDTO {
    private Map<String, RespuestaFurag> respuestas = new LinkedHashMap<>();
    private List<FuragPreguntaRespuestaDTO> detalle = List.of();

    public FuragDTO() {
    }

    public FuragDTO(Map<String, RespuestaFurag> respuestas) {
        setRespuestas(respuestas);
    }

    public Map<String, RespuestaFurag> respuestas() {
        return respuestas;
    }

    public Map<String, RespuestaFurag> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(Map<String, RespuestaFurag> respuestas) {
        this.respuestas = respuestas != null ? new LinkedHashMap<>(respuestas) : new LinkedHashMap<>();
    }

    public List<FuragPreguntaRespuestaDTO> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<FuragPreguntaRespuestaDTO> detalle) {
        this.detalle = detalle != null ? List.copyOf(detalle) : List.of();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return respuestas == null || respuestas.isEmpty();
    }
}
