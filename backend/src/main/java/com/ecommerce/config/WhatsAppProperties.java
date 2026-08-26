package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    private boolean enabled = false;
    private String accountSid = "";
    private String authToken = "";
    private String fromNumber = "";
    private String contentSid = "";

    public boolean isFullyConfigured() {
        return enabled
                && accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }

    public String normalisedFromNumber() {
        if (fromNumber == null || fromNumber.isBlank()) {
            return "";
        }
        return fromNumber.startsWith("whatsapp:") ? fromNumber : "whatsapp:" + fromNumber;
    }
}
