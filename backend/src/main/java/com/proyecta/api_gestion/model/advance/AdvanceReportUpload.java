package com.proyecta.api_gestion.model.advance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advance_report_uploads", schema = "proyecta_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "periodo"}))
public class AdvanceReportUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_id", nullable = false, length = 40)
    private String projectId;

    @Column(name = "periodo", nullable = false, length = 20)
    private String periodo;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by", nullable = false, length = 120)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    public AdvanceReportUpload() {}

    public AdvanceReportUpload(String projectId, String periodo, String fileName, String filePath, Long fileSize, String uploadedBy) {
        this.projectId = projectId;
        this.periodo = periodo;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }

    public Long getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getPeriodo() { return periodo; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public Long getFileSize() { return fileSize; }
    public String getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
