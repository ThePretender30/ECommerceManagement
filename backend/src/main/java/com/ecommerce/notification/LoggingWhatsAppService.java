package com.ecommerce.notification;

import lombok.extern.slf4j.Slf4j;

/**
 * The fallback sender used when Twilio credentials are absent or sending is disabled.
 *
 * <p>This is what makes the project runnable and demonstrable on a machine with no Twilio
 * account: every message that <em>would</em> have been sent is logged in full and still
 * recorded in the {@code notifications} table with status {@code SKIPPED}. Order flows,
 * the admin notification screen, and the smoke test all behave identically - only the
 * network call is missing.
 */
@Slf4j
public class LoggingWhatsAppService implements WhatsAppService {

    private final String reason;

    public LoggingWhatsAppService(String reason) {
        this.reason = reason;
        log.warn("""

                ------------------------------------------------------------
                 WhatsApp sending is INACTIVE - {}
                 Messages will be logged and stored with status SKIPPED.
                 To enable real delivery, set these in backend/.env:
                   WHATSAPP_ENABLED=true
                   TWILIO_ACCOUNT_SID=...
                   TWILIO_AUTH_TOKEN=...
                   TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
                ------------------------------------------------------------
                """, reason);
    }

    @Override
    public SendResult send(String toPhoneNumber, String message) {
        log.info("""

                [WhatsApp - NOT SENT]
                  To      : {}
                  Message : {}
                """, toPhoneNumber, message);
        return SendResult.skipped(reason);
    }

    @Override
    public String providerName() {
        return "LOGGING";
    }
}
