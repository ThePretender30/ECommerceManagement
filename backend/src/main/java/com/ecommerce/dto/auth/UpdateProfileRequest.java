package com.ecommerce.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        @Pattern(regexp = "^[a-zA-Z]+(?: [a-zA-Z]+)*$",
                 message = "Full name can only contain letters and spaces (no dots, digits, or symbols)")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{0,3}\\d{9,10}$",
                 message = "Phone number must include a valid country code (e.g. +91) and a 9 or 10-digit mobile number")
        String phoneNumber
) {}
