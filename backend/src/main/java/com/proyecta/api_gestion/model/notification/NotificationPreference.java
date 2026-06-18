package com.proyecta.api_gestion.model.notification;

import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preference", schema = "proyecta_db",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_preference_user_event_scope", columnNames = {"user_id", "event_code", "project_id"}))
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SeguridadUsuario user;

    @Column(name = "event_code", length = 120, nullable = false)
    private String eventCode;

    @Column(name = "project_id", length = 30)
    private String projectId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SeguridadUsuario getUser() { return user; }
    public void setUser(SeguridadUsuario user) { this.user = user; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
}
