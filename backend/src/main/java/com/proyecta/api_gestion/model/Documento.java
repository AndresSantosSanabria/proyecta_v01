package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.config.TipoDocumentoConfig;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proyecto_id", length = 30, nullable = false)
    private String proyectoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 30, nullable = false)
    private TipoDocumento tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_config_id", referencedColumnName = "tipo_documento_id")
    private TipoDocumentoConfig tipoDocumentoConfig;

    @Column(name = "nombre_original", length = 255, nullable = false)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", length = 255, nullable = false, unique = true)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", length = 500, nullable = false)
    private String rutaAlmacenamiento;

    @Column(name = "url_descarga", length = 500)
    private String urlDescarga;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Documento() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaCarga == null) {
            this.fechaCarga = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
    public void setRutaAlmacenamiento(String rutaAlmacenamiento) { this.rutaAlmacenamiento = rutaAlmacenamiento; }

    public String getUrlDescarga() { return urlDescarga; }
    public void setUrlDescarga(String urlDescarga) { this.urlDescarga = urlDescarga; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public TipoDocumentoConfig getTipoDocumentoConfig() { return tipoDocumentoConfig; }
    public void setTipoDocumentoConfig(TipoDocumentoConfig tipoDocumentoConfig) { this.tipoDocumentoConfig = tipoDocumentoConfig; }

    public String getTipoDocumentoCodigo() {
        if (tipoDocumentoConfig != null) return tipoDocumentoConfig.getCodigo();
        if (tipoDocumento != null) return tipoDocumento.name();
        return null;
    }
}
