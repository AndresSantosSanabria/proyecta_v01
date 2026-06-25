package com.proyecta.api_gestion.service.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public SpringMailNotificationSenderAdapter(JavaMailSender javaMailSender, com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository usuarioRepository) {
        this.javaMailSender = javaMailSender;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void send(String to, NotificationMessage message) {
        try {
            String targetEmail = to;
            if (!to.contains("@")) {
                var userOpt = usuarioRepository.findByUsernameIgnoreCase(to);
                if (userOpt.isPresent() && userOpt.get().getCorreo() != null && !userOpt.get().getCorreo().isBlank()) {
                    targetEmail = userOpt.get().getCorreo();
                } else {
                    logger.warn("No email found for user: {}", to);
                    return;
                }
            }

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(targetEmail);
            helper.setSubject(message.subject());
            helper.setText(message.body(), message.html());
            
            javaMailSender.send(mimeMessage);
            logger.info("Notification email sent successfully to: {}", targetEmail);
        } catch (Exception e) {
            logger.error("Failed to send notification email to: {}", to, e);
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage(), e);
        }
    }
}
