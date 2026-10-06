package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

import com.proyecta.api_gestion.application.port.out.persistence.notification.InAppNotificationRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long>, InAppNotificationRepositoryPort {
    Page<InAppNotification> findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(String username, Pageable pageable);
    Page<InAppNotification> findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(String username, Boolean readStatus, Pageable pageable);
    Page<InAppNotification> findByRecipientUsernameIgnoreCaseAndEventCodeOrderByCreatedAtDesc(String username, String eventCode, Pageable pageable);
    long countByRecipientUsernameIgnoreCaseAndReadStatusFalse(String username);
    boolean existsByRecipientIdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(Long recipientId, String eventCode, String sourceEntityId, LocalDateTime createdAtAfter);

    @Override
    default PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(String username, PageQuery pageable) {
        return PageBridge.toResult(findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username, PageBridge.toPageable(pageable)), pageable);
    }

    @Override
    default PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(String username, Boolean readStatus, PageQuery pageable) {
        return PageBridge.toResult(findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(username, readStatus, PageBridge.toPageable(pageable)), pageable);
    }

    @Override
    default PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseAndEventCodeOrderByCreatedAtDesc(String username, String eventCode, PageQuery pageable) {
        return PageBridge.toResult(findByRecipientUsernameIgnoreCaseAndEventCodeOrderByCreatedAtDesc(username, eventCode, PageBridge.toPageable(pageable)), pageable);
    }
}
