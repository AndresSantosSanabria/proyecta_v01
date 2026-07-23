package com.proyecta.api_gestion.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SpringMailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(SpringMailNotificationSenderAdapter.class);

    private final JavaMailSender javaMailSender;
    private final NotificationMailDispatchTracker tracker;

    @Value("${mail.from:}")
    private String fromEmail;

    @Value("${mail.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public SpringMailNotificationSenderAdapter(JavaMailSender javaMailSender,
                                               NotificationMailDispatchTracker tracker) {
        this.javaMailSender = javaMailSender;
        this.tracker = tracker;
    }

    @Override
    public NotificationSendResult send(String to, NotificationMessage message) {
        if (!notificationsEnabled) {
            tracker.skipped(to, message.subject(), "Notificaciones de correo desactivadas por configuración");
            logger.warn("[Mail] Envío omitido porque mail.notifications.enabled=false");
            return NotificationSendResult.skipped("mail.notifications.enabled=false");
        }

        tracker.sending(to, message.subject());
        logger.info("[Mail] Preparando envío SMTP | to={} | from={} | subject={} | html={}",
                to, fromEmail, message.subject(), message.html());

        try {
            doSend(to, message);
            tracker.sent(to, message.subject(), null);
            logger.info("[Mail] Correo enviado correctamente a {}", to);
            return NotificationSendResult.sent(null);
        } catch (MailAuthenticationException e) {
            String detail = "Fallo de autenticación SMTP: " + safeMessage(e);
            tracker.failed(to, message.subject(), detail);
            logger.error("[Mail] {}", detail, e);
            return NotificationSendResult.failed(detail);
        } catch (MailSendException e) {
            String detail = "Fallo al enviar SMTP: " + safeMessage(e);
            tracker.failed(to, message.subject(), detail);
            logger.error("[Mail] {}", detail, e);
            return NotificationSendResult.failed(detail);
        } catch (MailException e) {
            String detail = "Error SMTP: " + safeMessage(e);
            tracker.failed(to, message.subject(), detail);
            logger.error("[Mail] {}", detail, e);
            return NotificationSendResult.failed(detail);
        } catch (MessagingException e) {
            String detail = "Error MIME: " + safeMessage(e);
            tracker.failed(to, message.subject(), detail);
            logger.error("[Mail] {}", detail, e);
            return NotificationSendResult.failed(detail);
        } catch (Exception e) {
            String detail = "Error inesperado de correo: " + safeMessage(e);
            tracker.failed(to, message.subject(), detail);
            logger.error("[Mail] {}", detail, e);
            return NotificationSendResult.failed(detail);
        }
    }

    private void doSend(String to, NotificationMessage message) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(resolveFromEmail());
        helper.setTo(to);
        helper.setSubject(message.subject());
        helper.setText(message.body(), message.html());
        javaMailSender.send(mimeMessage);
    }

    private String resolveFromEmail() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail;
        }
        if (javaMailSender instanceof JavaMailSenderImpl mailSender) {
            String username = mailSender.getUsername();
            if (username != null && !username.isBlank()) {
                return username;
            }
        }
        return "no-reply@localhost";
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
