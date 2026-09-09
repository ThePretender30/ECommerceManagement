package com.ecommerce.email;

public record SendResult(String status, String messageId, String errorMessage) {

    public static SendResult sent(String messageId) {
        return new SendResult("SENT", messageId, null);
    }

    public static SendResult skipped(String reason) {
        return new SendResult("SKIPPED", null, reason);
    }

    public static SendResult failed(String error) {
        return new SendResult("FAILED", null, error);
    }
}
