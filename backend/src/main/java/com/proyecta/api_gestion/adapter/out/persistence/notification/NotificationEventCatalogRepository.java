package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationEventCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationEventCatalogRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface NotificationEventCatalogRepository extends JpaRepository<NotificationEventCatalog, String>, NotificationEventCatalogRepositoryPort {
    @Override
    default PageResult<NotificationEventCatalog> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

}
