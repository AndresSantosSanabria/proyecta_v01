package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.dto.config.FuragPreguntaRespuestaDTO;
import com.proyecta.api_gestion.model.enums.RespuestaFurag;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Embeddable
public class Furag {

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_infraestructura_datos", length = 10)
    private RespuestaFurag infraestructuraDatos;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_interoperabilidad", length = 10)
    private RespuestaFurag interoperabilidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_digitalizacion_automatizacion", length = 10)
    private RespuestaFurag digitalizacionAutomatizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_contratacion_publica", length = 10)
    private RespuestaFurag contratacionPublica;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_servicios_nube", length = 10)
    private RespuestaFurag serviciosNube;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_sandbox", length = 10)
    private RespuestaFurag sandbox;

    @Enumerated(EnumType.STRING)
    @Column(name = "furag_tecnologias_emergentes", length = 10)
    private RespuestaFurag tecnologiasEmergentes;

    @Transient
    private Map<String, RespuestaFurag> respuestas = new LinkedHashMap<>();

    @Transient
    private List<FuragPreguntaRespuestaDTO> detalle;

    public Furag() {
    }

    // Getters and Setters
    public RespuestaFurag getInfraestructuraDatos() {
        return infraestructuraDatos;
    }

    public void setInfraestructuraDatos(RespuestaFurag infraestructuraDatos) {
        this.infraestructuraDatos = infraestructuraDatos;
    }

    public RespuestaFurag getInteroperabilidad() {
        return interoperabilidad;
    }

    public void setInteroperabilidad(RespuestaFurag interoperabilidad) {
        this.interoperabilidad = interoperabilidad;
    }

    public RespuestaFurag getDigitalizacionAutomatizacion() {
        return digitalizacionAutomatizacion;
    }

    public void setDigitalizacionAutomatizacion(RespuestaFurag digitalizacionAutomatizacion) {
        this.digitalizacionAutomatizacion = digitalizacionAutomatizacion;
    }

    public RespuestaFurag getContratacionPublica() {
        return contratacionPublica;
    }

    public void setContratacionPublica(RespuestaFurag contratacionPublica) {
        this.contratacionPublica = contratacionPublica;
    }

    public RespuestaFurag getServiciosNube() {
        return serviciosNube;
    }

    public void setServiciosNube(RespuestaFurag serviciosNube) {
        this.serviciosNube = serviciosNube;
    }

    public RespuestaFurag getSandbox() {
        return sandbox;
    }

    public void setSandbox(RespuestaFurag sandbox) {
        this.sandbox = sandbox;
    }

    public RespuestaFurag getTecnologiasEmergentes() {
        return tecnologiasEmergentes;
    }

    public void setTecnologiasEmergentes(RespuestaFurag tecnologiasEmergentes) {
        this.tecnologiasEmergentes = tecnologiasEmergentes;
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
        this.detalle = detalle;
    }
}
