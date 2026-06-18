package com.proyecta.api_gestion.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_event_catalog", schema = "proyecta_db")
public class NotificationEventCatalog {

    @Id
    @Column(name = "code", length = 120, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 40, nullable = false)
    private String category;

    @Column(name = "default_enabled", nullable = false)
    private Boolean defaultEnabled = true;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "requires_project_context", nullable = false)
    private Boolean requiresProjectContext = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public NotificationEventCatalog() {
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getDefaultEnabled() { return defaultEnabled; }
    public void setDefaultEnabled(Boolean defaultEnabled) { this.defaultEnabled = defaultEnabled; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getRequiresProjectContext() { return requiresProjectContext; }
    public void setRequiresProjectContext(Boolean requiresProjectContext) { this.requiresProjectContext = requiresProjectContext; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
