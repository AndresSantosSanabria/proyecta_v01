package com.proyecta.api_gestion.domain.model.closure;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_closure_template", schema = "proyecta_db")
public class ClosureTemplate extends ClosureRegistroBase {

    @Column(name = "codigo_proceso", nullable = false, length = 30)
    private String codigoProceso;

    @Column(name = "version_num", nullable = false)
    private Integer versionNum;

    @Column(name = "nombre_documento", nullable = false, length = 200)
    private String nombreDocumento;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_json", nullable = false, columnDefinition = "jsonb")
    private String templateJson;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "updated_by", length = 80)
    private String updatedBy;

    public String getCodigoProceso() { return codigoProceso; }
    public void setCodigoProceso(String codigoProceso) { this.codigoProceso = codigoProceso; }
    public Integer getVersionNum() { return versionNum; }
    public void setVersionNum(Integer versionNum) { this.versionNum = versionNum; }
    public String getNombreDocumento() { return nombreDocumento; }
    public void setNombreDocumento(String nombreDocumento) { this.nombreDocumento = nombreDocumento; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public String getTemplateJson() { return templateJson; }
    public void setTemplateJson(String templateJson) { this.templateJson = templateJson; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
