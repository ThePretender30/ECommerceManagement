package com.ecommerce.exception;

/** Thrown when creating something that must be unique and already exists. Maps to 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
