package com.ecommerce.email;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class LoggingEmailService implements EmailService {

    private final String reason;

    public LoggingEmailService(String reason) {
        this.reason = reason;
        log.warn("""

                ------------------------------------------------------------
                 Email sending is in LOGGING mode - {}
                 Verification emails and OTPs will be displayed in this console.
                ------------------------------------------------------------
                """, reason);
    }

    @Override
    public SendResult send(String toEmail, String subject, String bodyText) {
        return send(toEmail, subject, bodyText, null);
    }

    @Override
    public SendResult send(String toEmail, String subject, String bodyText, String bodyHtml) {
        log.info("""

                ============================================================
                [EMAIL - LOGGING MODE]
                  To      : {}
                  Subject : {}
                  Body    :
                {}
                ============================================================
                """, toEmail, subject, bodyText != null ? bodyText : bodyHtml);
        return SendResult.sent("LOG-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Override
    public String providerName() {
        return "LOGGING";
    }
}
