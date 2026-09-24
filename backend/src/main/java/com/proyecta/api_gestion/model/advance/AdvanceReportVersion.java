package com.proyecta.api_gestion.model.advance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advance_report_version", schema = "proyecta_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"upload_id", "numero_version"}))
public class AdvanceReportVersion {

    public static final String ESTADO_ACTUAL = "ACTUAL";
    public static final String ESTADO_HISTORICA = "HISTORICA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "upload_id", nullable = false)
    private Long uploadId;

    @Column(name = "project_id", nullable = false, length = 40)
    private String projectId;

    @Column(name = "periodo", nullable = false, length = 20)
    private String periodo;

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "observacion", length = 1000)
    private String observacion;

    @Column(name = "subido_por", length = 120)
    private String subidoPor;

    @Column(name = "subido_rol", length = 60)
    private String subidoRol;

    @Column(name = "subido_en")
    private LocalDateTime subidoEn;

    public AdvanceReportVersion() {}

    public AdvanceReportVersion(Long uploadId, String projectId, String periodo, Integer numeroVersion,
                                String fileName, String filePath, Long fileSize, String mimeType,
                                String subidoPor, String subidoRol) {
        this.uploadId = uploadId;
        this.projectId = projectId;
        this.periodo = periodo;
        this.numeroVersion = numeroVersion;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.estado = ESTADO_ACTUAL;
        this.subidoPor = subidoPor;
        this.subidoRol = subidoRol;
        this.subidoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUploadId() { return uploadId; }
    public String getProjectId() { return projectId; }
    public String getPeriodo() { return periodo; }
    public Integer getNumeroVersion() { return numeroVersion; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getSubidoPor() { return subidoPor; }
    public String getSubidoRol() { return subidoRol; }
    public LocalDateTime getSubidoEn() { return subidoEn; }
}
