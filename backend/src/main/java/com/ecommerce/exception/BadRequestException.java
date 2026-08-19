package com.ecommerce.exception;

/** Thrown when the request is well-formed but violates a business rule. Maps to 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
