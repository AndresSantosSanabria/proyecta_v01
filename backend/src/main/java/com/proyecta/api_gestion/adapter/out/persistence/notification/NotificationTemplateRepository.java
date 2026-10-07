package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationTemplateRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String>, NotificationTemplateRepositoryPort {
    @Override
    default PageResult<NotificationTemplate> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    long countByEnabledTrue();
}
