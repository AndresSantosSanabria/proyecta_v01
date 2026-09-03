package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationAuditRepository extends JpaRepository<NotificationAudit, UUID> {
    long countByChannelAndStatus(String channel, String status);

    long countByStatus(String status);

    Page<NotificationAudit> findByStatus(String status, Pageable pageable);

    Page<NotificationAudit> findByChannelAndStatus(String channel, String status, Pageable pageable);

    Page<NotificationAudit> findByEventCodeAndStatus(String eventCode, String status, Pageable pageable);

    Page<NotificationAudit> findByRecipientContainingIgnoreCaseAndStatus(String recipient, String status, Pageable pageable);

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
}
