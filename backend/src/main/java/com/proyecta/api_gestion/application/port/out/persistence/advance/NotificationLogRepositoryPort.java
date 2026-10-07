package com.proyecta.api_gestion.application.port.out.persistence.advance;

import com.proyecta.api_gestion.domain.model.advance.NotificationLog;
import java.time.LocalDate;

public interface NotificationLogRepositoryPort {

    boolean existsByProjectIdAndNotificationDateAndNotificationType(String projectId, LocalDate notificationDate, String notificationType);

    <E extends NotificationLog> E save(E entity);
}

