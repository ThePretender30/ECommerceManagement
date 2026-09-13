package com.ecommerce.config;

import com.ecommerce.email.BrevoEmailService;
import com.ecommerce.email.EmailService;
import com.ecommerce.email.LoggingEmailService;
import com.ecommerce.email.SmtpEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MailConfig {

    private final MailProperties mailProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public EmailService emailService() {
        if (!mailProperties.isEnabled()) {
            return new LoggingEmailService("app.mail.enabled is false");
        }

        String fromEmail = (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank() && !mailProperties.getFrom().contains("no-reply@rozbazaar.local"))
                ? mailProperties.getFrom().trim()
                : (mailProperties.getUsername() != null && !mailProperties.getUsername().isBlank())
                        ? mailProperties.getUsername().trim()
                        : "no-reply@rozbazaar.local";

        String fromName = (mailProperties.getFromName() != null && !mailProperties.getFromName().isBlank())
                ? mailProperties.getFromName().trim()
                : "Roz Bazaar";

        // 1. Prefer Brevo HTTPS REST API if API key is provided (works on Render and all cloud hosts without SMTP port blocks)
        if (mailProperties.getBrevoApiKey() != null && !mailProperties.getBrevoApiKey().isBlank()) {
            log.info("Initialized Brevo HTTPS API email sender (from={})", fromEmail);
            return new BrevoEmailService(mailProperties.getBrevoApiKey(), fromEmail, fromName, objectMapper);
        }

        // 2. Standard SMTP fallback
        if (mailProperties.getHost() == null || mailProperties.getHost().isBlank()) {
            return new LoggingEmailService("neither Brevo API key nor SMTP host is configured");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailProperties.getHost().trim());
        sender.setPort(mailProperties.getPort());

        if (mailProperties.getUsername() != null && !mailProperties.getUsername().isBlank()) {
            sender.setUsername(mailProperties.getUsername().trim());
        }
        if (mailProperties.getPassword() != null && !mailProperties.getPassword().isBlank()) {
            sender.setPassword(mailProperties.getPassword().trim());
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(mailProperties.getUsername() != null && !mailProperties.getUsername().isBlank()));
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        int port = mailProperties.getPort();
        if (port == 465) {
            // Port 465: SMTPS / Direct SSL
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            // Port 587 or others: STARTTLS
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        log.info("Initialized SMTP email sender (host={}, port={}, user={}, from={})",
                mailProperties.getHost(), mailProperties.getPort(), mailProperties.getUsername(), fromEmail);

        return new SmtpEmailService(sender, fromEmail, fromName);
    }
}
