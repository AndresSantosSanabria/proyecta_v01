package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUser_UsernameIgnoreCaseAndEventCodeAndProjectId(String username, String eventCode, String projectId);
    List<NotificationPreference> findByUser_UsernameIgnoreCase(String username);
    List<NotificationPreference> findByEventCodeAndProjectId(String eventCode, String projectId);
}
