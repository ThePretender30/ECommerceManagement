package com.ecommerce.dto.auth;

public record LoginChallengeResponse(
        boolean verificationRequired,
        String sessionToken,
        String maskedEmail,
        long expiresInSeconds
) {
    public static LoginChallengeResponse of(String sessionToken, String maskedEmail, long expiresInSeconds) {
        return new LoginChallengeResponse(true, sessionToken, maskedEmail, expiresInSeconds);
    }
}
