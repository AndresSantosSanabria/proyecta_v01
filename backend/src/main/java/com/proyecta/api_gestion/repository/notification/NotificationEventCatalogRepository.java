package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationEventCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventCatalogRepository extends JpaRepository<NotificationEventCatalog, String> {
}
