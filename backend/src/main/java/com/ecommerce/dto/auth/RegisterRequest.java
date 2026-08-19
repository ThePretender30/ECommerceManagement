package com.ecommerce.dto.auth;

import jakarta.validation.constraints.*;

/**
 * Sign-up payload.
 *
 * <p>Deliberately has <b>no role field</b>. New accounts always receive ROLE_CUSTOMER,
 * assigned server-side, so a client cannot register itself as an administrator by adding
 * {@code "role": "ROLE_ADMIN"} to the request body.
 */
public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 180, message = "Email must not exceed 180 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        /*
         * E.164 is required because this number is the WhatsApp notification target and
         * Twilio rejects anything else.
         */
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                 message = "Phone number must be in international format, e.g. +919876543210")
        String phoneNumber
) {}
