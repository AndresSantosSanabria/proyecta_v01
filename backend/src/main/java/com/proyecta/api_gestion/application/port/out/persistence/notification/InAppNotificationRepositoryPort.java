package com.proyecta.api_gestion.application.port.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.InAppNotification;
import java.time.LocalDateTime;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;

public interface InAppNotificationRepositoryPort {

    java.util.List<InAppNotification> findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(
            String username, Boolean readStatus);


    PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(String username, PageQuery pageable);

    PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(String username, Boolean readStatus, PageQuery pageable);

    PageResult<InAppNotification> findByRecipientUsernameIgnoreCaseAndEventCodeOrderByCreatedAtDesc(String username, String eventCode, PageQuery pageable);

    long countByRecipientUsernameIgnoreCaseAndReadStatusFalse(String username);

    boolean existsByRecipientIdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(Long recipientId, String eventCode, String sourceEntityId, LocalDateTime createdAtAfter);

    <E extends InAppNotification> E save(E entity);

    <E extends InAppNotification> java.util.List<E> saveAll(Iterable<E> entities);

    java.util.Optional<InAppNotification> findById(Long id);
}

