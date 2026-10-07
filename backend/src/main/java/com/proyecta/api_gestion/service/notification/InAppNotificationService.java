package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.dto.notification.InAppNotificationDTO;
import com.proyecta.api_gestion.domain.model.notification.InAppNotification;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.notification.InAppNotificationRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.domain.value.PageQuery;
import com.proyecta.api_gestion.domain.value.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class InAppNotificationService {
    private final InAppNotificationRepositoryPort repository;
    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;

    public InAppNotificationService(InAppNotificationRepositoryPort repository, SeguridadUsuarioRepositoryPort usuarioRepositoryPort) {
        this.repository = repository;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Transactional
    public InAppNotification create(String usernameOrEmail, String title, String message, String eventCode, String severity, String sourceEntityId, String targetUrl) {
        SeguridadUsuario user = usuarioRepositoryPort.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> usuarioRepositoryPort.findByCorreoIgnoreCase(usernameOrEmail))
                .orElse(null);
        if (user == null) {
            return null;
        }
        InAppNotification notification = new InAppNotification();
        notification.setRecipient(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventCode(eventCode);
        notification.setSeverity(severity);
        notification.setSourceEntityId(sourceEntityId);
        notification.setTargetUrl(targetUrl);
        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResult<InAppNotificationDTO> list(String username, Boolean readStatus, String eventCode, PageQuery query) {
        if (eventCode != null && !eventCode.isBlank()) {
            return repository.findByRecipientUsernameIgnoreCaseAndEventCodeOrderByCreatedAtDesc(username, eventCode, query)
                    .map(this::toDto);
        }
        if (readStatus != null) {
            return repository.findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(username, readStatus, query)
                    .map(this::toDto);
        }
        return repository.findByRecipientUsernameIgnoreCaseOrderByCreatedAtDesc(username, query)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return repository.countByRecipientUsernameIgnoreCaseAndReadStatusFalse(username);
    }

    @Transactional(readOnly = true)
    public boolean existsForRecipient(Long recipientId, String eventCode, String sourceEntityId, LocalDateTime createdAtAfter) {
        return repository.existsByRecipientIdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(
                recipientId, eventCode, sourceEntityId, createdAtAfter);
    }

    @Transactional
    public void markRead(Long id, String requesterUsername) {
        InAppNotification notification = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada: " + id));
        // CWE-639: verificacion de dueno. Se responde con el mismo mensaje que
        // cuando el id no existe para no revelar la existencia de notificaciones ajenas.
        if (notification.getRecipient() == null
                || requesterUsername == null
                || !requesterUsername.equalsIgnoreCase(notification.getRecipient().getUsername())) {
            throw new IllegalArgumentException("Notificacion no encontrada: " + id);
        }
        notification.setReadStatus(true);
        notification.setReadAt(LocalDateTime.now(ZoneId.systemDefault()));
        repository.save(notification);
    }

    @Transactional
    public void markAllRead(String username) {
        List<InAppNotification> unread = repository.findByRecipientUsernameIgnoreCaseAndReadStatusOrderByCreatedAtDesc(username, false);
        for (InAppNotification notification : unread) {
            notification.setReadStatus(true);
            notification.setReadAt(LocalDateTime.now(ZoneId.systemDefault()));
        }
        repository.saveAll(unread);
    }

    private InAppNotificationDTO toDto(InAppNotification notification) {
        return new InAppNotificationDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEventCode(),
                notification.getReadStatus(),
                notification.getCreatedAt(),
                notification.getSeverity(),
                notification.getTargetUrl()
        );
    }
}
