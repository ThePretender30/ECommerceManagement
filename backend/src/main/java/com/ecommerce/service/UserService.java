package com.ecommerce.service;

import com.ecommerce.dto.auth.UserResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile self-service for customers, and the user list for admins. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------- Customer self-service ----------------

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    /**
     * Updates the caller's own name and phone.
     *
     * <p>Email is intentionally not editable here: it is the login identifier and the
     * subject of every issued token, so changing it would need a re-verification flow.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, String fullName, String phoneNumber) {
        User user = findUserOrThrow(userId);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            // This is also the WhatsApp destination, so it must stay in E.164 form.
            if (!phoneNumber.trim().matches("^\\+[1-9]\\d{7,14}$")) {
                throw new BadRequestException(
                        "Phone number must be in international format, e.g. +919876543210");
            }
            user.setPhoneNumber(phoneNumber.trim());
        }

        return UserResponse.from(userRepository.save(user));
    }

    /** Changes the caller's password after confirming they know the current one. */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Your current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters long.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new DuplicateResourceException("New password must be different from the current one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User {} changed their password", userId);
    }

    // ---------------- Admin ----------------

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return PagedResponse.from(
                userRepository.searchUsers(query == null ? "" : query.trim(), pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserForAdmin(Long userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    /**
     * Enables or disables an account.
     *
     * <p>Disabling is preferred over deletion: the user's orders and reviews stay intact,
     * and {@code JwtAuthenticationFilter} re-checks {@code enabled} on every request, so
     * access stops immediately even if they still hold a valid token.
     */
    @Transactional
    public UserResponse setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId) && !enabled) {
            throw new BadRequestException("You cannot disable your own administrator account.");
        }

        User user = findUserOrThrow(userId);
        user.setEnabled(enabled);
        log.info("Admin {} {} user {}", adminId, enabled ? "enabled" : "disabled", userId);
        return UserResponse.from(userRepository.save(user));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
