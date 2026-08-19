package com.ecommerce.entity;

/** Outcome of a single notification delivery attempt. */
public enum NotificationStatus {
    /** Accepted by the provider; {@code providerMessageId} is populated. */
    SENT,
    /** The provider rejected it or the call threw; {@code errorMessage} explains why. */
    FAILED,
    /** Sending was disabled or credentials were absent - the message was logged only. */
    SKIPPED
}
