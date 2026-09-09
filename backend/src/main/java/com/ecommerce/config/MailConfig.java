package com.ecommerce.config;

import com.ecommerce.email.EmailService;
import com.ecommerce.email.LoggingEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

        log.info("Email service initialized in LOGGING mode (host={})", mailProperties.getHost());
        return new LoggingEmailService("SMTP credentials not provided; defaulting to logging");
    }
}
