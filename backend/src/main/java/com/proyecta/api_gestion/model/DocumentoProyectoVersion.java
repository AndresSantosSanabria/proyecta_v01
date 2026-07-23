package com.proyecta.api_gestion.model;

import com.proyecta.api_gestion.model.enums.DocumentoProyectoVersionEstado;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_proyecto_version")
public class DocumentoProyectoVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_proyecto_version_id")
    private Long id;

    @Column(name = "proyecto_id", nullable = false, length = 30)
    private String proyectoId;

    @Column(name = "tipo_documento", nullable = false, length = 30)
    private String tipoDocumento;

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "nombre_archivo_original", nullable = false, length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "nombre_almacenado", nullable = false, length = 255)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private DocumentoProyectoVersionEstado estado = DocumentoProyectoVersionEstado.ACTUAL;

    @Column(name = "subido_por", length = 200)
    private String subidoPor;

    @Column(name = "subido_rol", length = 80)
    private String subidoRol;

    @Column(name = "subido_en", nullable = false, updatable = false)
    private LocalDateTime subidoEn;

    @PrePersist
    void onCreate() {
        if (subidoEn == null) {
            subidoEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Integer getNumeroVersion() { return numeroVersion; }
    public void setNumeroVersion(Integer numeroVersion) { this.numeroVersion = numeroVersion; }

    public String getNombreArchivoOriginal() { return nombreArchivoOriginal; }
    public void setNombreArchivoOriginal(String nombreArchivoOriginal) { this.nombreArchivoOriginal = nombreArchivoOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
    public void setRutaAlmacenamiento(String rutaAlmacenamiento) { this.rutaAlmacenamiento = rutaAlmacenamiento; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public DocumentoProyectoVersionEstado getEstado() { return estado; }
    public void setEstado(DocumentoProyectoVersionEstado estado) { this.estado = estado; }

    public String getSubidoPor() { return subidoPor; }
    public void setSubidoPor(String subidoPor) { this.subidoPor = subidoPor; }

    public String getSubidoRol() { return subidoRol; }
    public void setSubidoRol(String subidoRol) { this.subidoRol = subidoRol; }

    public LocalDateTime getSubidoEn() { return subidoEn; }
    public void setSubidoEn(LocalDateTime subidoEn) { this.subidoEn = subidoEn; }
}
