package com.ecommerce.notification;

public interface WhatsAppService {

    SendResult send(String toPhoneNumber, String message);

    String providerName();

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
