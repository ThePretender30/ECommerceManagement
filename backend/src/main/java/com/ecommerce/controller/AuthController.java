package com.ecommerce.controller;

import com.ecommerce.dto.auth.*;
import com.ecommerce.dto.common.MessageResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Registration, login, OTP verification, and the signed-in user's own profile.
 *
 * <p>Controllers in this project stay thin: validate the request shape, hand the work to a
 * service, and map the result to a response. There is no database access here.
 *
 * <p>{@code @AuthenticationPrincipal} supplies the caller's identity from the validated
 * JWT. Services take that id rather than trusting any user id sent in the request, which
 * is what stops one account acting on another's behalf.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, sign in, verify OTP, and manage your account")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Create a customer account and return a JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Initiate customer sign in and dispatch an email OTP")
    public ResponseEntity<LoginChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "Initiate admin portal sign in and dispatch an email OTP")
    public ResponseEntity<LoginChallengeResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify the email OTP and return the JWT session")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend a new 6-digit OTP code to the user's email")
    public ResponseEntity<LoginChallengeResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendLoginOtp(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Fetch the signed-in user's profile")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the signed-in user's name and phone number")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                userService.updateProfile(principal.getId(), request.fullName(), request.phoneNumber()));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the signed-in user's password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("Your password has been updated."));
    }

    /**
     * Logout is a client-side action for stateless JWT auth: the browser discards the
     * token. This endpoint exists so the frontend has something explicit to call, and so
     * the behaviour is documented rather than surprising.
     */
    @PostMapping("/logout")
    @Operation(summary = "Acknowledge sign-out (the client discards its token)")
    public ResponseEntity<MessageResponse> logout() {
        return ResponseEntity.ok(MessageResponse.of("Signed out successfully."));
    }
}
