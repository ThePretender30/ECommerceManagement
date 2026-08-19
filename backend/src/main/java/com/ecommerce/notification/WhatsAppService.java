package com.ecommerce.notification;

/**
 * Sends a WhatsApp message.
 *
 * <p>The interface exists so the rest of the application never depends on Twilio. Exactly
 * one implementation is wired in at startup: {@link TwilioWhatsAppService} when credentials
 * are configured, {@link LoggingWhatsAppService} otherwise. Swapping to a different
 * provider - or adding SMS or email - means adding an implementation, not editing
 * {@code OrderService}.
 */
public interface WhatsAppService {

    /**
     * Attempts delivery. Implementations must not throw: a failed notification is a
     * recorded outcome, never an exception that could disturb order processing.
     *
     * @param toPhoneNumber recipient in E.164 form, e.g. {@code +919876543210}
     * @param message       the message body
     */
    SendResult send(String toPhoneNumber, String message);

    /** Names the active implementation, shown in logs and the admin notification view. */
    String providerName();

    /**
     * Outcome of one delivery attempt.
     *
     * @param providerMessageId provider reference on success (Twilio message SID)
     * @param errorMessage      why it failed, on failure
     */
    record SendResult(
            Outcome outcome,
            String providerMessageId,
            String errorMessage
    ) {
        public enum Outcome { SENT, FAILED, SKIPPED }

        public static SendResult sent(String providerMessageId) {
            return new SendResult(Outcome.SENT, providerMessageId, null);
        }

        public static SendResult failed(String errorMessage) {
            return new SendResult(Outcome.FAILED, null, errorMessage);
        }

        public static SendResult skipped(String reason) {
            return new SendResult(Outcome.SKIPPED, null, reason);
        }
    }
}
