package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationEventCatalog;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface NotificationEventCatalogRepositoryPort {

    java.util.Optional<NotificationEventCatalog> findById(String id);

    java.util.List<NotificationEventCatalog> findAll();

    long count();

    PageResult<NotificationEventCatalog> findAll(PageQuery query);
}
