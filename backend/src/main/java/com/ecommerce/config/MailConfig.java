package com.ecommerce.config;

import com.ecommerce.email.EmailService;
import com.ecommerce.email.LoggingEmailService;
import com.ecommerce.email.SmtpEmailService;
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

    @Bean
    public EmailService emailService() {
        if (!mailProperties.isEnabled()) {
            return new LoggingEmailService("app.mail.enabled is false");
        }

        if (mailProperties.getHost() == null || mailProperties.getHost().isBlank()) {
            return new LoggingEmailService("app.mail.host is not configured");
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
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        String fromEmail = (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank() && !mailProperties.getFrom().contains("no-reply@rozbazaar.local"))
                ? mailProperties.getFrom().trim()
                : (mailProperties.getUsername() != null && !mailProperties.getUsername().isBlank())
                        ? mailProperties.getUsername().trim()
                        : "no-reply@rozbazaar.local";

        String fromName = (mailProperties.getFromName() != null && !mailProperties.getFromName().isBlank())
                ? mailProperties.getFromName().trim()
                : "Roz Bazaar";

        log.info("Initialized SMTP email sender (host={}, port={}, user={}, from={})",
                mailProperties.getHost(), mailProperties.getPort(), mailProperties.getUsername(), fromEmail);

        return new SmtpEmailService(sender, fromEmail, fromName);
    }
}
