package com.ecommerce.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank(message = "Session token is required")
        String sessionToken
) {}
