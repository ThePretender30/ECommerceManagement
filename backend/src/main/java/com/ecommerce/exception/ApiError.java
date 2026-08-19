package com.ecommerce.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape returned by every failing endpoint.
 *
 * <p>Because it is consistent, the React axios interceptor can render any backend error
 * with one code path, and {@code fieldErrors} lets forms highlight the exact input that
 * failed validation.
 */
@Getter
@Builder
public class ApiError {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /** Field name to message, populated only for bean-validation failures. */
    private final Map<String, String> fieldErrors;
}
