package com.proyecta.api_gestion.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_template", schema = "proyecta_db")
public class NotificationTemplate {
    @Id
    @Column(name = "event_code", length = 120, nullable = false, updatable = false)
    private String eventCode;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "html_enabled", nullable = false)
    private Boolean htmlEnabled = false;

    @Column(name = "severity", length = 30)
    private String severity = "INFO";

    @Column(name = "scope", length = 30)
    private String scope = "GLOBAL";

    @Column(name = "subject_template", length = 500, nullable = false)
    private String subjectTemplate;

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    private String bodyTemplate;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getHtmlEnabled() { return htmlEnabled; }
    public void setHtmlEnabled(Boolean htmlEnabled) { this.htmlEnabled = htmlEnabled; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public void setSubjectTemplate(String subjectTemplate) { this.subjectTemplate = subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
