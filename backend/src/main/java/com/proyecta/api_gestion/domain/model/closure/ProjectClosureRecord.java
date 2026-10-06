package com.proyecta.api_gestion.domain.model.closure;

import com.proyecta.api_gestion.domain.model.Proyecto;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_closures", schema = "proyecta_db")
public class ProjectClosureRecord extends ClosureRegistroBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ClosureTemplate template;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_snapshot", nullable = false, columnDefinition = "jsonb")
    private String templateSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", nullable = false, columnDefinition = "jsonb")
    private String formData;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    public Proyecto getProyecto() { return proyecto; }
    public void setProyecto(Proyecto proyecto) { this.proyecto = proyecto; }
    public ClosureTemplate getTemplate() { return template; }
    public void setTemplate(ClosureTemplate template) { this.template = template; }
    public String getTemplateSnapshot() { return templateSnapshot; }
    public void setTemplateSnapshot(String templateSnapshot) { this.templateSnapshot = templateSnapshot; }
    public String getFormData() { return formData; }
    public void setFormData(String formData) { this.formData = formData; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
