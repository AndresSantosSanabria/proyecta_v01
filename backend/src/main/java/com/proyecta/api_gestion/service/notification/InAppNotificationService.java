package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.dto.notification.InAppNotificationDTO;
import com.proyecta.api_gestion.model.notification.InAppNotification;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.notification.InAppNotificationRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InAppNotificationService {
    private final InAppNotificationRepository repository;
    private final SeguridadUsuarioRepository usuarioRepository;

    public InAppNotificationService(InAppNotificationRepository repository, SeguridadUsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public InAppNotification create(String usernameOrEmail, String title, String message, String eventCode, String severity, String sourceEntityId, String targetUrl) {
        SeguridadUsuario user = usuarioRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(usernameOrEmail))
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
    public Page<InAppNotificationDTO> list(String username, Pageable pageable) {
        return repository.findByRecipient_UsernameIgnoreCaseOrderByCreatedAtDesc(username, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return repository.countByRecipient_UsernameIgnoreCaseAndReadStatusFalse(username);
    }

    @Transactional(readOnly = true)
    public boolean existsForRecipient(Long recipientId, String eventCode, String sourceEntityId, LocalDateTime createdAtAfter) {
        return repository.existsByRecipient_IdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(
                recipientId, eventCode, sourceEntityId, createdAtAfter);
    }

    @Transactional
    public void markRead(Long id) {
        InAppNotification notification = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada: " + id));
        notification.setReadStatus(true);
        notification.setReadAt(LocalDateTime.now());
        repository.save(notification);
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
