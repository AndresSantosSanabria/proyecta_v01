package com.proyecta.api_gestion.service.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SpringMailNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger logger = LoggerFactory.getLogger(SpringMailNotificationSenderAdapter.class);

    private final JavaMailSender javaMailSender;
    private final com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository usuarioRepository;

    @Value("${mail.from:notificaciones@gobierno.gov.co}")
    private String fromEmail;

    public SpringMailNotificationSenderAdapter(JavaMailSender javaMailSender,
            com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository usuarioRepository) {
        this.javaMailSender = javaMailSender;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void send(String to, NotificationMessage message) {
        logger.debug("[Mail] Iniciando envio de correo a destinatario: '{}'", to);

        // Resolver correo si se recibe username en lugar de email
        String targetEmail = to;
        if (!to.contains("@")) {
            logger.debug("[Mail] Destinatario '{}' no es un correo, buscando en repositorio...", to);
            var userOpt = usuarioRepository.findByUsernameIgnoreCase(to);
            if (userOpt.isPresent() && userOpt.get().getCorreo() != null && !userOpt.get().getCorreo().isBlank()) {
                targetEmail = userOpt.get().getCorreo();
                logger.debug("[Mail] Correo resuelto para '{}': {}", to, targetEmail);
            } else {
                logger.warn("[Mail] No se encontro correo para el usuario: '{}'. Notificacion omitida.", to);
                return;
            }
        }

        logger.info("[Mail] Enviando correo a: {} | Asunto: '{}'", targetEmail, message.subject());

        // Intento principal
        try {
            doSend(targetEmail, message);
            logger.info("[Mail] ✓ Correo enviado exitosamente a: {} | Asunto: '{}'", targetEmail, message.subject());
            return;
        } catch (MailException | jakarta.mail.MessagingException e) {
            logger.warn("[Mail] Primer intento fallido para '{}': {}. Reintentando en 3s...", targetEmail,
                    e.getMessage());
        }

        // Reintento unico tras breve pausa (da tiempo a recuperar la conexion)
        try {
            Thread.sleep(3000);
            doSend(targetEmail, message);
            logger.info("[Mail] ✓ Correo enviado en reintento a: {} | Asunto: '{}'", targetEmail, message.subject());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error("[Mail] x Reintento interrumpido para: {}", targetEmail);
            throw new RuntimeException("Envio interrumpido para: " + targetEmail, ie);
        } catch (Exception e) {
            logger.error("[Mail] x Fallo definitivo al enviar correo a: {} | Error: {}", targetEmail, e.getMessage(),
                    e);
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }

    private void doSend(String targetEmail, NotificationMessage message) throws jakarta.mail.MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(targetEmail);
        helper.setSubject(message.subject());
        helper.setText(message.body(), message.html());
        javaMailSender.send(mimeMessage);
    }
}
