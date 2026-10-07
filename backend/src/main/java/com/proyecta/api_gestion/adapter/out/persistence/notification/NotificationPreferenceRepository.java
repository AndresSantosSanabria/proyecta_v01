package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationPreferenceRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long>, NotificationPreferenceRepositoryPort {
    @Override
    default PageResult<NotificationPreference> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }

    Optional<NotificationPreference> findByUserUsernameIgnoreCaseAndEventCodeAndProjectId(String username, String eventCode, String projectId);
}
