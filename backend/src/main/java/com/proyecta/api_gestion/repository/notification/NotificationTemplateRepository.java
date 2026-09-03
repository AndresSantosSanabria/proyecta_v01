package com.proyecta.api_gestion.repository.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    long countByEnabledTrue();

    @Query("SELECT t FROM NotificationTemplate t " +
           "JOIN com.proyecta.api_gestion.model.notification.NotificationEventCatalog e ON t.eventCode = e.code " +
           "WHERE (:category IS NULL OR :category = '' OR e.category = :category) " +
           "AND (:severity IS NULL OR :severity = '' OR t.severity = :severity) " +
           "AND (:enabled IS NULL OR t.enabled = :enabled) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(t.eventCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<NotificationTemplate> findAllFiltered(
            @Param("category") String category,
            @Param("severity") String severity,
            @Param("enabled") Boolean enabled,
            @Param("search") String search,
            Pageable pageable);
}
