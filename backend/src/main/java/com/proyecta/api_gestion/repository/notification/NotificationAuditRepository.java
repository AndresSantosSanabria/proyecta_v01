package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, UUID> {
}
