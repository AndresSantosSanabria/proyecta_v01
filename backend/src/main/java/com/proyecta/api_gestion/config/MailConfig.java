package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuracion explicita de JavaMailSender para Office 365.
 *
 * DIAGNOSTICO DE RED:
 *  - Puerto 465 (SMTPS): BLOQUEADO por firewall corporativo (TCP timeout en todos los IPs)
 *  - Puerto 587 (STARTTLS): ABIERTO — TCP conecta pero el banner "220" tarda en llegar
 *
 * CAUSA DEL SocketTimeoutException:
 *  El timeout ocurre leyendo el banner SMTP inicial ("220 Microsoft ESMTP").
 *  Office 365 aplica rate limiting por IP en conexiones SMTP frecuentes.
 *  Cuando hay muchos reintentos o conexiones en poco tiempo, el servidor
 *  demora la respuesta del banner hasta que el socket cliente caduca.
 *
 * SOLUCION APLICADA:
 *  1. Forzar preferencia IPv6 (curl confirmo que funciona, Java usaba IPv4)
 *  2. Incrementar connectiontimeout a 90s para absorber la demora del banner
 *  3. Deshabilitar pool de conexiones (cada correo abre y cierra su propia conexion)
 *  4. quitwait=false para no esperar el "221" de cierre del servidor
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        // Forzar preferencia IPv6 — curl confirmo que la ruta IPv6 responde mas rapido
        System.setProperty("java.net.preferIPv6Addresses", "true");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Office 365 SMTP con STARTTLS (unico puerto abierto en la red corporativa)
        sender.setHost("smtp.office365.com");
        sender.setPort(587);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setProtocol("smtp");
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();

        // STARTTLS obligatorio para Office 365 en puerto 587
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", "smtp.office365.com");

        // Autenticacion LOGIN (mecanismo que acepta Office 365)
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.auth.mechanisms", "LOGIN");

        // Timeouts amplios para absorber la demora del banner en Office 365
        // El connectiontimeout cubre la espera del "220" inicial
        props.put("mail.smtp.connectiontimeout", "90000");  // 90s para el TCP + banner
        props.put("mail.smtp.timeout", "60000");            // 60s para operaciones SMTP
        props.put("mail.smtp.writetimeout", "60000");       // 60s para escritura

        // NO reutilizar conexiones — cada correo abre su propia sesion TCP
        // Evita el problema de "stale connections" entre notificaciones
        props.put("mail.smtp.quitwait", "false");

        log.info("[MailConfig] JavaMailSender configurado -> smtp://smtp.office365.com:587 (STARTTLS) | usuario: {}", username);
        log.info("[MailConfig] IPv6 preferido: {} | connectiontimeout: 90s",
                System.getProperty("java.net.preferIPv6Addresses"));

        return sender;
    }
}
