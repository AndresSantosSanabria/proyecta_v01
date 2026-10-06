package com.proyecta.api_gestion.adapter.out.persistence.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationMailDispatchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationMailDispatchLogRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import com.proyecta.api_gestion.adapter.out.persistence.PageBridge;
public interface NotificationMailDispatchLogRepository extends JpaRepository<NotificationMailDispatchLog, UUID>, NotificationMailDispatchLogRepositoryPort {
    @Override
    default PageResult<NotificationMailDispatchLog> findAll(PageQuery query) {
        return PageBridge.toResult(findAll(PageBridge.toPageable(query)), query);
    }


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

    @Override
    default PageResult<NotificationMailDispatchLog> findFailedWithFilters(String status, String recipient, Instant from, Instant to, PageQuery pageable) {
        return PageBridge.toResult(findFailedWithFilters(status, recipient, from, to, PageBridge.toPageable(pageable)), pageable);
    }
}
