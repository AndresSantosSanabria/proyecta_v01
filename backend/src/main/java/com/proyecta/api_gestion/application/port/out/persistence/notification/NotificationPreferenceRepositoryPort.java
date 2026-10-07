package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationPreference;
import java.util.Optional;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface NotificationPreferenceRepositoryPort {

    Optional<NotificationPreference> findByUserUsernameIgnoreCaseAndEventCodeAndProjectId(String username, String eventCode, String projectId);

    <E extends NotificationPreference> E save(E entity);

    java.util.List<NotificationPreference> findAll();

    PageResult<NotificationPreference> findAll(PageQuery query);
}
