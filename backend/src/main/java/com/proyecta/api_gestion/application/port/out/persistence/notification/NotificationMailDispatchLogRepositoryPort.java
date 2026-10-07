package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationMailDispatchLog;
import java.time.Instant;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface NotificationMailDispatchLogRepositoryPort {

    PageResult<NotificationMailDispatchLog> findFailedWithFilters(String status, String recipient, Instant from, Instant to, PageQuery pageable);

    <E extends NotificationMailDispatchLog> E save(E entity);

    java.util.List<NotificationMailDispatchLog> findAll();

    PageResult<NotificationMailDispatchLog> findAll(PageQuery query);
}

