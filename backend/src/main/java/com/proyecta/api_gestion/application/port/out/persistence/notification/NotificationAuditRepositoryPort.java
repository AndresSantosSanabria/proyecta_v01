package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationAudit;
import java.time.LocalDateTime;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface NotificationAuditRepositoryPort {

    long countByChannelAndStatus(String channel, String status);

    PageResult<NotificationAudit> findFailedWithFilters(String status, String channel, String eventCode, String recipient, LocalDateTime from, LocalDateTime to, PageQuery pageable);

    <E extends NotificationAudit> E save(E entity);
}

