package com.proyecta.api_gestion.repository.advance;

import com.proyecta.api_gestion.model.advance.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByProjectIdAndNotificationDateAndNotificationType(
            String projectId, LocalDate notificationDate, String notificationType);
}
