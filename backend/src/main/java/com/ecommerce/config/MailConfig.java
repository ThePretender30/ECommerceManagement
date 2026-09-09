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
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        log.info("Initialized SMTP email sender (host={}, port={}, user={})",
                mailProperties.getHost(), mailProperties.getPort(), mailProperties.getUsername());

        return new SmtpEmailService(sender, mailProperties.getFrom(), mailProperties.getFromName());
    }
}
