package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Envía notificaciones por correo agrupadas en un único hilo de conversación
 * por proyecto (patrón Outlook: Message-ID raíz + In-Reply-To / References).
 *
 * Solo se persiste email_message_id cuando el envío SMTP fue exitoso, de modo
 * que un fallo no deje un hilo "fantasma" en la base de datos.
 */
@Service
public class NotificacionEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailService.class);

    private final ProyectoRepository proyectoRepository;
    private final JavaMailSender javaMailSender;

    @Value("${mail.from:}")
    private String fromEmail;

    @Value("${mail.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public NotificacionEmailService(ProyectoRepository proyectoRepository,
                                    JavaMailSender javaMailSender) {
        this.proyectoRepository = proyectoRepository;
        this.javaMailSender = javaMailSender;
    }

    @Transactional
    public String sendProjectThreadedEmail(String projectId, String mensaje, String actorUsername) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new BadRequestException("El mensaje del correo es obligatorio.");
        }
        if (!notificationsEnabled) {
            throw new BadRequestException("El envío de correos está deshabilitado (mail.notifications.enabled=false).");
        }

        String normalizedId = projectId == null ? null : projectId.trim().toUpperCase(Locale.ROOT);
        Proyecto proyecto = proyectoRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        Set<String> recipients = resolveRecipients(proyecto);
        if (recipients.isEmpty()) {
            throw new BadRequestException(
                    "El proyecto no tiene destinatarios de correo (director o gestor sin correo).");
        }

        boolean isFirstMessage = proyecto.getEmailMessageId() == null
                || proyecto.getEmailMessageId().isBlank();
        String rootMessageId = isFirstMessage
                ? generateRootMessageId(proyecto.getId())
                : proyecto.getEmailMessageId();
        String subject = buildSubject(proyecto, isFirstMessage);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(resolveFromEmail());
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(mensaje.trim(), false);

            // Message-ID propio de este envío (siempre único por mensaje).
            String currentMessageId = generateRootMessageId(proyecto.getId());
            mimeMessage.setHeader("Message-ID", currentMessageId);

            if (!isFirstMessage) {
                // Encadenar al hilo raíz del proyecto.
                mimeMessage.setHeader("In-Reply-To", rootMessageId);
                mimeMessage.setHeader("References", rootMessageId);
            }

            javaMailSender.send(mimeMessage);

            if (isFirstMessage) {
                proyecto.setEmailMessageId(currentMessageId);
                proyectoRepository.save(proyecto);
            }

            log.info("[EmailThread] Correo enviado para proyecto {} | first={} | recipients={} | actor={}",
                    proyecto.getId(), isFirstMessage, recipients.size(), actorUsername);
            return currentMessageId;
        } catch (Exception e) {
            log.error("[EmailThread] Error enviando correo para proyecto {}: {}",
                    proyecto.getId(), e.getMessage(), e);
            throw new BadRequestException("No fue posible enviar el correo: " + e.getMessage());
        }
    }

    private Set<String> resolveRecipients(Proyecto proyecto) {
        Set<String> recipients = new LinkedHashSet<>();
        String directorCorreo = trimToNull(proyecto.getCorreoDirector());
        if (directorCorreo != null) {
            recipients.add(directorCorreo);
        }
        String gestor = trimToNull(proyecto.getRegistradoInicialPor());
        if (gestor != null && gestor.contains("@")) {
            recipients.add(gestor);
        }
        return recipients;
    }

    private String buildSubject(Proyecto proyecto, boolean isFirstMessage) {
        String base = "Proyecto " + nullSafe(proyecto.getId())
                + " - " + nullSafe(proyecto.getNombre());
        if (isFirstMessage) {
            return base;
        }
        // Evitar doble "Re:" si el llamador ya lo incluye.
        String trimmed = base.trim();
        if (trimmed.regionMatches(true, 0, "Re:", 0, 3)) {
            return trimmed;
        }
        return "Re: " + trimmed;
    }

    private String generateRootMessageId(String projectId) {
        String domain = resolveDomain();
        String safeProject = projectId == null ? "unknown" : projectId.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
        return "<" + safeProject + "-" + UUID.randomUUID() + "@" + domain + ">";
    }

    private String resolveDomain() {
        String from = trimToNull(fromEmail);
        if (from != null && from.contains("@")) {
            return from.substring(from.indexOf('@') + 1).trim();
        }
        if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
            String username = mailSender.getUsername();
            if (username != null && username.contains("@")) {
                return username.substring(username.indexOf('@') + 1).trim();
            }
        }
        return "cundinamarca.gov.co";
    }

    private String resolveFromEmail() {
        String from = trimToNull(fromEmail);
        if (from != null) {
            return from;
        }
        if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
            String username = mailSender.getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        return "no-reply@localhost";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
