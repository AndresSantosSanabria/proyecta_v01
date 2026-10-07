package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flujo comun de los schedulers de notificaciones de entregables: resolucion de
 * destinatarios, deduplicacion y publicacion del evento en-app.
 */
public abstract class EntregableNotificationSchedulerSupport {

    private static final Logger log = LoggerFactory.getLogger(EntregableNotificationSchedulerSupport.class);

    protected final EntregableRepositoryPort entregableRepositoryPort;
    protected final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    protected final InAppNotificationService inAppNotificationService;
    protected final NotificationEventPublisherPort notificationPublisher;
    protected final SystemParameterService systemParameterService;

    protected EntregableNotificationSchedulerSupport(
            EntregableRepositoryPort entregableRepositoryPort,
            SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
            InAppNotificationService inAppNotificationService,
            NotificationEventPublisherPort notificationPublisher,
            SystemParameterService systemParameterService) {
        this.entregableRepositoryPort = entregableRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.inAppNotificationService = inAppNotificationService;
        this.notificationPublisher = notificationPublisher;
        this.systemParameterService = systemParameterService;
    }

    protected abstract NotificationEventType eventType();

    protected abstract long calcularDias(LocalDate today, Entregable entregable);

    protected abstract String claveDias();

    protected abstract String tituloNotificacion(long dias, Entregable entregable);

    protected void procesarCada(List<Entregable> pendientes, LocalDate today, LocalDateTime cutoffForDedup, String tag) {
        for (Entregable entregable : pendientes) {
            try {
                procesarEntregable(entregable, today, cutoffForDedup);
            } catch (Exception ex) {
                log.warn("[{}] Failed to process entregable {}: {}", tag, entregable.getId(), ex.getMessage());
            }
        }
    }

    private void procesarEntregable(Entregable entregable, LocalDate today,
                                    LocalDateTime cutoffForDedup) {
        String projectId = entregable.getHito().getFase().getProyecto().getId();
        String director = entregable.getHito().getFase().getProyecto().getCorreoDirector();

        if (director == null || director.isBlank()) {
            return;
        }

        List<String> recipients = ProjectNotificationRecipients.resolve(entregable.getHito().getFase().getProyecto());
        if (recipients.isEmpty()) {
            return;
        }

        var recipient = usuarioRepositoryPort.findByUsernameIgnoreCase(director)
                .or(() -> usuarioRepositoryPort.findByCorreoIgnoreCase(director))
                .orElse(null);
        if (recipient == null) {
            return;
        }

        String sourceEntityId = String.valueOf(entregable.getId());
        String eventCode = eventType().name();
        if (inAppNotificationService.existsForRecipient(recipient.getId(), eventCode, sourceEntityId, cutoffForDedup)) {
            return;
        }

        long dias = calcularDias(today, entregable);
        String projectName = entregable.getHito().getFase().getProyecto().getNombre();

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("entregableNombre", entregable.getNombre());
        datos.put("entregableId", entregable.getId());
        datos.put("projectName", projectName);
        datos.put(claveDias(), dias);
        datos.put("fechaLimite", entregable.getFechaLimite().toString());
        datos.put("recipients", recipients);
        datos.put("title", tituloNotificacion(dias, entregable));

        notificationPublisher.publish(new NotificationContext(
                eventType(),
                projectId,
                "system",
                datos));
    }

    protected static long diasEntre(LocalDate desde, LocalDate hasta) {
        return ChronoUnit.DAYS.between(desde, hasta);
    }
}
