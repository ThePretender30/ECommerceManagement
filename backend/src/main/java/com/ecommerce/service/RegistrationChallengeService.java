package com.ecommerce.service;

import com.ecommerce.dto.auth.LoginChallengeResponse;
import com.ecommerce.dto.auth.RegisterRequest;
import com.ecommerce.email.EmailService;
import com.ecommerce.email.SendResult;
import com.ecommerce.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationChallengeService {

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, RegistrationChallenge> challenges = new ConcurrentHashMap<>();

    public record RegistrationChallenge(
            String fullName,
            String email,
            String passwordHash,
            String phoneNumber,
            String otpHash,
            Instant expiresAt,
            int attemptsRemaining
    ) {}

    public LoginChallengeResponse createChallenge(RegisterRequest request, String passwordHash) {
        cleanExpiredChallenges();

        String sessionToken = UUID.randomUUID().toString();
        String plainOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(plainOtp);

        Instant expiresAt = Instant.now().plus(OTP_EXPIRY);
        challenges.put(sessionToken, new RegistrationChallenge(
                request.fullName().trim(),
                request.email().trim().toLowerCase(),
                passwordHash,
                request.phoneNumber().trim(),
                otpHash,
                expiresAt,
                MAX_ATTEMPTS
        ));

        sendOtpEmail(request.email().trim().toLowerCase(), request.fullName().trim(), plainOtp);

        return LoginChallengeResponse.of(sessionToken, maskEmail(request.email().trim()), OTP_EXPIRY.toSeconds());
    }

    public LoginChallengeResponse resendOtp(String sessionToken) {
        cleanExpiredChallenges();

        RegistrationChallenge existing = challenges.get(sessionToken);
        if (existing == null || Instant.now().isAfter(existing.expiresAt())) {
            challenges.remove(sessionToken);
            throw new BadRequestException("The verification session has expired. Please sign up again.");
        }

        String plainOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(plainOtp);
        Instant expiresAt = Instant.now().plus(OTP_EXPIRY);

        challenges.put(sessionToken, new RegistrationChallenge(
                existing.fullName(),
                existing.email(),
                existing.passwordHash(),
                existing.phoneNumber(),
                otpHash,
                expiresAt,
                MAX_ATTEMPTS
        ));

        sendOtpEmail(existing.email(), existing.fullName(), plainOtp);

        return LoginChallengeResponse.of(sessionToken, maskEmail(existing.email()), OTP_EXPIRY.toSeconds());
    }

    public RegistrationChallenge verifyAndConsume(String sessionToken, String rawOtp) {
        cleanExpiredChallenges();

        RegistrationChallenge challenge = challenges.get(sessionToken);
        if (challenge == null || Instant.now().isAfter(challenge.expiresAt())) {
            challenges.remove(sessionToken);
            throw new BadRequestException("Verification code has expired or is invalid. Please sign up again.");
        }

        if (challenge.attemptsRemaining() <= 0) {
            challenges.remove(sessionToken);
            throw new BadRequestException("Too many invalid attempts. Please sign up again.");
        }

        if (!passwordEncoder.matches(rawOtp, challenge.otpHash())) {
            int remaining = challenge.attemptsRemaining() - 1;
            if (remaining <= 0) {
                challenges.remove(sessionToken);
                throw new BadRequestException("Invalid verification code. Maximum attempts exceeded, please sign up again.");
            }
            challenges.put(sessionToken, new RegistrationChallenge(
                    challenge.fullName(),
                    challenge.email(),
                    challenge.passwordHash(),
                    challenge.phoneNumber(),
                    challenge.otpHash(),
                    challenge.expiresAt(),
                    remaining
            ));
            throw new BadRequestException("Invalid verification code. You have " + remaining + " attempts remaining.");
        }

        challenges.remove(sessionToken);
        return challenge;
    }

    private void sendOtpEmail(String email, String fullName, String otp) {
        String subject = "Verify your email for Roz Bazaar: " + otp;
        String body = String.format("""
                Hello %s,

                Thank you for creating an account with Roz Bazaar.
                Your 6-digit email verification code is:

                  %s

                This code is valid for 5 minutes.
                If you did not request this account, you can ignore this email.

                Regards,
                Roz Bazaar Team
                """, fullName, otp);

        SendResult result = emailService.send(email, subject, body);
        if ("FAILED".equals(result.status())) {
            log.warn("""

                    ------------------------------------------------------------
                    Failed to dispatch email via {} to {}: {}
                    FALLBACK OTP CODE: {}
                    ------------------------------------------------------------
                    """, emailService.providerName(), email, result.errorMessage(), otp);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    private void cleanExpiredChallenges() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }
}
