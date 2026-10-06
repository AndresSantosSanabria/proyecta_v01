package com.proyecta.api_gestion.adapter.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import com.proyecta.api_gestion.application.port.out.persistence.advance.NotificationLogRepositoryPort;
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, NotificationLogRepositoryPort {
    boolean existsByProjectIdAndNotificationDateAndNotificationType(
            String projectId, LocalDate notificationDate, String notificationType);
}

