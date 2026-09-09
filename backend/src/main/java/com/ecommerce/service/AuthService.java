package com.ecommerce.service;

import com.ecommerce.dto.auth.*;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.RoleName;
import com.ecommerce.entity.User;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtService;
import com.ecommerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginChallengeService loginChallengeService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists. Try signing in instead.");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_CUSTOMER is missing. The database was not seeded correctly."));

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber().trim())
                .enabled(true)
                .roles(Set.of(customerRole))
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new customer account: {}", saved.getEmail());

        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginChallengeResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getEmail()));

        log.info("Credentials verified for customer login: {}", user.getEmail());
        return loginChallengeService.createChallenge(user, false);
    }

    @Transactional(readOnly = true)
    public LoginChallengeResponse loginAdmin(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getEmail()));

        if (!user.hasRole(RoleName.ROLE_ADMIN)) {
            log.warn("Non-admin user attempted admin login: {}", user.getEmail());
            throw new AccessDeniedException("Access denied: This account does not have administrator privileges.");
        }

        log.info("Credentials verified for admin login: {}", user.getEmail());
        return loginChallengeService.createChallenge(user, true);
    }

    @Transactional(readOnly = true)
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        LoginChallengeService.Challenge challenge = loginChallengeService.verifyAndConsume(
                request.sessionToken(), request.otp().trim());

        User user = userRepository.findById(challenge.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", challenge.userId()));

        if (challenge.adminRequired() && !user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Access denied: This account does not have administrator privileges.");
        }

        log.info("Successful OTP login for user: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    public LoginChallengeResponse resendLoginOtp(ResendOtpRequest request) {
        return loginChallengeService.resendOtp(request.sessionToken());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String token = jwtService.generateToken(user.getId(), user.getEmail(), roleNames);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
