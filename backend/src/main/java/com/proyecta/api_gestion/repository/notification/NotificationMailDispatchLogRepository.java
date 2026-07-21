package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationMailDispatchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationMailDispatchLogRepository extends JpaRepository<NotificationMailDispatchLog, UUID> {
}
