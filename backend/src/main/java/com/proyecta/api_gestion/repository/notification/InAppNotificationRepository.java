package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {
    Page<InAppNotification> findByRecipient_UsernameIgnoreCaseOrderByCreatedAtDesc(String username, Pageable pageable);
    long countByRecipient_UsernameIgnoreCaseAndReadStatusFalse(String username);
    boolean existsByRecipient_IdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(Long recipientId, String eventCode, String sourceEntityId, LocalDateTime createdAtAfter);
}
