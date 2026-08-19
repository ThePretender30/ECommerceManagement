package com.ecommerce.dto.common;

/** Simple acknowledgement for operations that have nothing meaningful to return. */
public record MessageResponse(String message) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
