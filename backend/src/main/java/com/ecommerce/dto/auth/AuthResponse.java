package com.ecommerce.dto.auth;

/**
 * Returned by register and login.
 *
 * <p>The user object travels with the token so the React app can render the navbar and
 * decide which routes to expose immediately, without a follow-up {@code /api/auth/me} call.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {
    public static AuthResponse of(String token, long expiresInMs, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
