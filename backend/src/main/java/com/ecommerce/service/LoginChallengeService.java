package com.ecommerce.service;

import com.ecommerce.dto.auth.LoginChallengeResponse;
import com.ecommerce.email.EmailService;
import com.ecommerce.entity.User;
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
public class LoginChallengeService {

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public record Challenge(
            Long userId,
            String email,
            String otpHash,
            Instant expiresAt,
            int attemptsRemaining,
            boolean adminRequired
    ) {}

    public LoginChallengeResponse createChallenge(User user, boolean adminRequired) {
        cleanExpiredChallenges();

        String sessionToken = UUID.randomUUID().toString();
        String plainOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(plainOtp);

        Instant expiresAt = Instant.now().plus(OTP_EXPIRY);
        challenges.put(sessionToken, new Challenge(user.getId(), user.getEmail(), otpHash, expiresAt, MAX_ATTEMPTS, adminRequired));

        sendOtpEmail(user.getEmail(), user.getFullName(), plainOtp);

        return LoginChallengeResponse.of(sessionToken, maskEmail(user.getEmail()), OTP_EXPIRY.toSeconds());
    }

    public LoginChallengeResponse resendOtp(String sessionToken) {
        cleanExpiredChallenges();

        Challenge existing = challenges.get(sessionToken);
        if (existing == null || Instant.now().isAfter(existing.expiresAt())) {
            challenges.remove(sessionToken);
            throw new BadRequestException("The verification session has expired. Please sign in again.");
        }

        String plainOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(plainOtp);
        Instant expiresAt = Instant.now().plus(OTP_EXPIRY);

        challenges.put(sessionToken, new Challenge(
                existing.userId(),
                existing.email(),
                otpHash,
                expiresAt,
                MAX_ATTEMPTS,
                existing.adminRequired()
        ));

        sendOtpEmail(existing.email(), "User", plainOtp);

        return LoginChallengeResponse.of(sessionToken, maskEmail(existing.email()), OTP_EXPIRY.toSeconds());
    }

    public Challenge verifyAndConsume(String sessionToken, String rawOtp) {
        cleanExpiredChallenges();

        Challenge challenge = challenges.get(sessionToken);
        if (challenge == null || Instant.now().isAfter(challenge.expiresAt())) {
            challenges.remove(sessionToken);
            throw new BadRequestException("Verification code has expired or is invalid. Please sign in again.");
        }

        if (challenge.attemptsRemaining() <= 0) {
            challenges.remove(sessionToken);
            throw new BadRequestException("Too many invalid attempts. Please sign in again.");
        }

        if (!passwordEncoder.matches(rawOtp, challenge.otpHash())) {
            int remaining = challenge.attemptsRemaining() - 1;
            if (remaining <= 0) {
                challenges.remove(sessionToken);
                throw new BadRequestException("Invalid verification code. Maximum attempts exceeded, please sign in again.");
            }
            challenges.put(sessionToken, new Challenge(
                    challenge.userId(),
                    challenge.email(),
                    challenge.otpHash(),
                    challenge.expiresAt(),
                    remaining,
                    challenge.adminRequired()
            ));
            throw new BadRequestException("Invalid verification code. You have " + remaining + " attempts remaining.");
        }

        challenges.remove(sessionToken);
        return challenge;
    }

    private void sendOtpEmail(String email, String fullName, String otp) {
        String subject = "Your Roz Bazaar Sign-In Verification Code: " + otp;
        String body = String.format("""
                Hello %s,

                Your 6-digit verification code to sign in to Roz Bazaar is:

                  %s

                This code is valid for 5 minutes.
                If you did not attempt to sign in, please secure your account immediately.

                Regards,
                Roz Bazaar Team
                """, fullName, otp);

        emailService.send(email, subject, body);
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
