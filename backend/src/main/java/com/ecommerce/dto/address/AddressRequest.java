package com.ecommerce.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(max = 120, message = "Recipient name must not exceed 120 characters")
        String fullName,

        @NotBlank(message = "Contact phone is required")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                 message = "Phone must be in international format, e.g. +919876543210")
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String line1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        String line2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        Boolean isDefault
) {}
