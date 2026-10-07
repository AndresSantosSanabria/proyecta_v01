package com.proyecta.api_gestion.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.time.ZoneId;

@MappedSuperclass
public abstract class DocumentoSubidaEnBase extends DocumentoSubidaBase {

    @Column(name = "subido_en", nullable = false, updatable = false)
    private LocalDateTime subidoEn;

    @PrePersist
    void onCreate() {
        if (subidoEn == null) {
            subidoEn = LocalDateTime.now(ZoneId.systemDefault());
        }
    }

    public LocalDateTime getSubidoEn() { return subidoEn; }
    public void setSubidoEn(LocalDateTime subidoEn) { this.subidoEn = subidoEn; }
}
