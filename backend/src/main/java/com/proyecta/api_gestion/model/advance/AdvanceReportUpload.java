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

    @Column(name = "verified_by", length = 120)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "returned_by", length = 120)
    private String returnedBy;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "subido_rol", length = 60)
    private String subidoRol;

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
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getReturnedBy() { return returnedBy; }
    public void setReturnedBy(String returnedBy) { this.returnedBy = returnedBy; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getSubidoRol() { return subidoRol; }
    public void setSubidoRol(String subidoRol) { this.subidoRol = subidoRol; }
}
