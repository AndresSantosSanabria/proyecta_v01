package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "documento_dinamico", indexes = {
        @Index(name = "idx_proyecto_tipo", columnList = "proyecto_id,tipo_documento")
})
public class DocumentoDinamico extends DocumentoCargaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", length = 100, nullable = false)
    private String tipoDocumento;

    public DocumentoDinamico() {
        // Constructor vacío intencional: lo requiere JPA para instanciar la entidad.
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
