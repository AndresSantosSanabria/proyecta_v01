package com.proyecta.api_gestion.model.advance;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log", schema = "proyecta_db",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "notification_date", "notification_type"}))
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "project_id", nullable = false, length = 40)
    private String projectId;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public NotificationLog() {}

    public NotificationLog(String projectId, LocalDate notificationDate, String notificationType) {
        this.projectId = projectId;
        this.notificationDate = notificationDate;
        this.notificationType = notificationType;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getProjectId() { return projectId; }
    public LocalDate getNotificationDate() { return notificationDate; }
    public String getNotificationType() { return notificationType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
