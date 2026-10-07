package com.proyecta.api_gestion.domain.model;

import com.proyecta.api_gestion.domain.model.enums.DocumentoVersionEstado;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "documento_version")
public class DocumentoVersion extends DocumentoSubidaEnBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_version_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "nombre_archivo_original", nullable = false, length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "archivo_storage", nullable = false, length = 300)
    private String archivoStorage;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "application/pdf";

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "comentario_carga", length = 1000)
    private String comentarioCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentoVersionEstado estado = DocumentoVersionEstado.ACTUAL;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Entregable getEntregable() { return entregable; }
    public void setEntregable(Entregable entregable) { this.entregable = entregable; }

    public Integer getNumeroVersion() { return numeroVersion; }
    public void setNumeroVersion(Integer numeroVersion) { this.numeroVersion = numeroVersion; }

    public String getNombreArchivoOriginal() { return nombreArchivoOriginal; }
    public void setNombreArchivoOriginal(String nombreArchivoOriginal) { this.nombreArchivoOriginal = nombreArchivoOriginal; }

    public String getArchivoStorage() { return archivoStorage; }
    public void setArchivoStorage(String archivoStorage) { this.archivoStorage = archivoStorage; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public String getComentarioCarga() { return comentarioCarga; }
    public void setComentarioCarga(String comentarioCarga) { this.comentarioCarga = comentarioCarga; }

    public DocumentoVersionEstado getEstado() { return estado; }
    public void setEstado(DocumentoVersionEstado estado) { this.estado = estado; }
}
