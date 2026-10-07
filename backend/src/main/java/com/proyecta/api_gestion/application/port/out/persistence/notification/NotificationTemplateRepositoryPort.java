package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface NotificationTemplateRepositoryPort {

    long countByEnabledTrue();

    <E extends NotificationTemplate> E save(E entity);

    java.util.Optional<NotificationTemplate> findById(String id);

    java.util.List<NotificationTemplate> findAll();

    PageResult<NotificationTemplate> findAll(PageQuery query);
}
