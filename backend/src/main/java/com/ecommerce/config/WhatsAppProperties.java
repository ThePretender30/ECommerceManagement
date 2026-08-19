package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio WhatsApp settings, bound from {@code app.whatsapp.*}.
 *
 * <p>All three credentials come from environment variables and default to empty.
 * {@link #isFullyConfigured()} is what decides at startup whether the real Twilio sender
 * or the logging fallback is wired in - so a missing credential degrades the feature
 * instead of crashing the application.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    /** Master switch. Even with credentials present, false keeps the app in log-only mode. */
    private boolean enabled = false;

    private String accountSid = "";

    private String authToken = "";

    /** Twilio sender, e.g. {@code whatsapp:+14155238886} (the sandbox number). */
    private String fromNumber = "";

    /** True only when sending is switched on AND every credential is present. */
    public boolean isFullyConfigured() {
        return enabled
                && accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }

    /** Twilio requires the {@code whatsapp:} scheme prefix on both sender and recipient. */
    public String normalisedFromNumber() {
        if (fromNumber == null || fromNumber.isBlank()) {
            return "";
        }
        return fromNumber.startsWith("whatsapp:") ? fromNumber : "whatsapp:" + fromNumber;
    }
}
