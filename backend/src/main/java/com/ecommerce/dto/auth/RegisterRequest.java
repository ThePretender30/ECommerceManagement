package com.ecommerce.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        @Pattern(regexp = "^[a-zA-Z]+(?: [a-zA-Z]+)*$",
                 message = "Full name can only contain letters and spaces (no dots, digits, or symbols)")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        @Size(max = 180, message = "Email must not exceed 180 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                 message = "Phone number must be in international format, e.g. +919876543210")
        String phoneNumber
) {}
