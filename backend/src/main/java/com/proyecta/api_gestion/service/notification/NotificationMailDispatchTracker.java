package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationMailDispatchLog;
import com.proyecta.api_gestion.repository.notification.NotificationMailDispatchLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NotificationMailDispatchTracker {

    public record Snapshot(
            NotificationDeliveryStatus status,
            String recipient,
            String subject,
            String detail,
            Instant updatedAt
    ) {}

    private final AtomicReference<Snapshot> state = new AtomicReference<>(
            new Snapshot(NotificationDeliveryStatus.IDLE, null, null, "Sin actividad", Instant.now())
    );
    private final NotificationMailDispatchLogRepository repository;

    public NotificationMailDispatchTracker(NotificationMailDispatchLogRepository repository) {
        this.repository = repository;
    }

    public Snapshot get() {
        return state.get();
    }

    public void sending(String recipient, String subject) {
        update(NotificationDeliveryStatus.SENDING, recipient, subject, "Enviando correo");
    }

    public void sent(String recipient, String subject, String providerMessageId) {
        update(NotificationDeliveryStatus.SENT, recipient, subject,
                providerMessageId != null ? "Enviado: " + providerMessageId : "Enviado");
    }

    public void failed(String recipient, String subject, String error) {
        update(NotificationDeliveryStatus.FAILED, recipient, subject,
                error != null ? error : "Error desconocido");
    }

    public void skipped(String recipient, String subject, String reason) {
        update(NotificationDeliveryStatus.SKIPPED, recipient, subject,
                reason != null ? reason : "Omitido");
    }

    private void update(NotificationDeliveryStatus status, String recipient, String subject, String detail) {
        Snapshot snapshot = new Snapshot(status, recipient, subject, detail, Instant.now());
        state.set(snapshot);
        NotificationMailDispatchLog log = new NotificationMailDispatchLog();
        log.setRecipient(recipient);
        log.setSubject(subject);
        log.setStatus(status.name());
        log.setDetail(detail);
        repository.save(log);
    }
}
