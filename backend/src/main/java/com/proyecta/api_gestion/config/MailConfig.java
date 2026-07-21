package com.proyecta.api_gestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.protocol:smtp}")
    private String protocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean authEnabled;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean startTlsEnabled;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean startTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:}")
    private String sslTrust;

    @Value("${spring.mail.properties.mail.smtp.ssl.protocols:TLSv1.2 TLSv1.3}")
    private String sslProtocols;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:30000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:30000}")
    private int readTimeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:30000}")
    private int writeTimeout;

    @Value("${spring.mail.properties.mail.smtp.quitwait:false}")
    private boolean quitWait;

    @Value("${spring.mail.properties.mail.smtp.auth.mechanisms:LOGIN}")
    private String authMechanisms;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setProtocol(protocol);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(authEnabled));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));
        props.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        props.put("mail.smtp.ssl.protocols", sslProtocols);
        props.put("mail.smtp.auth.mechanisms", authMechanisms);
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(readTimeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));
        props.put("mail.smtp.quitwait", String.valueOf(quitWait));

        if (sslTrust != null && !sslTrust.isBlank()) {
            props.put("mail.smtp.ssl.trust", sslTrust);
        }

        log.info("[MailConfig] SMTP listo -> {}://{}:{} | usuario={}", protocol, host, port, username);
        log.info("[MailConfig] TLS starttls.enable={} required={} | auth={} mechanisms={} | timeouts(ms): connect={}, read={}, write={}",
                startTlsEnabled, startTlsRequired, authEnabled, authMechanisms, connectionTimeout, readTimeout, writeTimeout);

        return sender;
    }
}
