package com.proyecta.api_gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "riesgo_solucion_adjunto")
public class RiesgoSolucionAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "riesgo_solucion_adjunto_id")
    private Long id;

    @Column(name = "nombre_original", nullable = false, length = 300)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, length = 300)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 120)
    private String rutaAlmacenamiento;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "fecha_carga", updatable = false)
    private LocalDateTime fechaCarga;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "riesgo_id", nullable = false)
    private Riesgo riesgo;

    public RiesgoSolucionAdjunto() {
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCarga == null) {
            fechaCarga = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getNombreAlmacenado() {
        return nombreAlmacenado;
    }

    public void setNombreAlmacenado(String nombreAlmacenado) {
        this.nombreAlmacenado = nombreAlmacenado;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public Riesgo getRiesgo() {
        return riesgo;
    }

    public void setRiesgo(Riesgo riesgo) {
        this.riesgo = riesgo;
    }
}
