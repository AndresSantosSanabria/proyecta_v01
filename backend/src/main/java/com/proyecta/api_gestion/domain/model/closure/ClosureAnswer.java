package com.proyecta.api_gestion.domain.model.closure;

import jakarta.persistence.*;

@Entity
@Table(name = "project_closure_answer", schema = "proyecta_db")
public class ClosureAnswer extends ClosureRegistroBase {

    @Column(name = "proyecto_id", nullable = false, length = 30)
    private String proyectoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private ClosureQuestion question;

    @Column(name = "respuesta", columnDefinition = "text")
    private String respuesta;

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }
    public ClosureQuestion getQuestion() { return question; }
    public void setQuestion(ClosureQuestion question) { this.question = question; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
}
