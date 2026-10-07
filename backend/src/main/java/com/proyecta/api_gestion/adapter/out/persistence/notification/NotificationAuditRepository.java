package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationAuditRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, UUID>, NotificationAuditRepositoryPort {
    long countByChannelAndStatus(String channel, String status);

    @Query("SELECT na FROM NotificationAudit na WHERE na.status = :status " +
            "AND (:channel IS NULL OR na.channel = :channel) " +
            "AND (:eventCode IS NULL OR na.eventCode = :eventCode) " +
            "AND (:recipient IS NULL OR LOWER(na.recipient) LIKE LOWER(CONCAT('%', :recipient, '%'))) " +
            "AND (:from IS NULL OR na.createdAt >= :from) " +
            "AND (:to IS NULL OR na.createdAt <= :to) " +
            "ORDER BY na.createdAt DESC")
    Page<NotificationAudit> findFailedWithFilters(
            @Param("status") String status,
            @Param("channel") String channel,
            @Param("eventCode") String eventCode,
            @Param("recipient") String recipient,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Override
    default PageResult<NotificationAudit> findFailedWithFilters(String status, String channel, String eventCode, String recipient, LocalDateTime from, LocalDateTime to, PageQuery pageable) {
        return PageBridge.toResult(findFailedWithFilters(status, channel, eventCode, recipient, from, to, PageBridge.toPageable(pageable)), pageable);
    }
}
