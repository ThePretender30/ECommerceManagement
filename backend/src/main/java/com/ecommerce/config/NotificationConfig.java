package com.ecommerce.config;

import com.ecommerce.notification.LoggingWhatsAppService;
import com.ecommerce.notification.TwilioWhatsAppService;
import com.ecommerce.notification.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class NotificationConfig {

    @Bean
    public WhatsAppService whatsAppService(WhatsAppProperties properties) {
        if (properties.isFullyConfigured()) {
            log.info("WhatsApp notifications ENABLED via Twilio.");
            return new TwilioWhatsAppService(properties);
        }

        String reason = !properties.isEnabled()
                ? "app.whatsapp.enabled is false"
                : "Twilio credentials are incomplete (check TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM)";

        return new LoggingWhatsAppService(reason);
    }
}
