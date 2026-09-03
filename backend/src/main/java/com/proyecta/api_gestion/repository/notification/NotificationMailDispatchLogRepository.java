package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationMailDispatchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationMailDispatchLogRepository extends JpaRepository<NotificationMailDispatchLog, UUID> {

    long countByStatus(String status);

    Page<NotificationMailDispatchLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT ndl FROM NotificationMailDispatchLog ndl WHERE ndl.status = :status " +
            "AND (:recipient IS NULL OR LOWER(ndl.recipient) LIKE LOWER(CONCAT('%', :recipient, '%'))) " +
            "AND (:from IS NULL OR ndl.createdAt >= :from) " +
            "AND (:to IS NULL OR ndl.createdAt <= :to) " +
            "ORDER BY ndl.createdAt DESC")
    Page<NotificationMailDispatchLog> findFailedWithFilters(
            @Param("status") String status,
            @Param("recipient") String recipient,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
