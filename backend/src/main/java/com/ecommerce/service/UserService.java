package com.ecommerce.service;

import com.ecommerce.dto.auth.UserResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductReview;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductReviewRepository productReviewRepository;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String fullName, String phoneNumber) {
        User user = findUserOrThrow(userId);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            if (!phoneNumber.trim().matches("^\\+[1-9]\\d{0,3}\\d{9,10}$")) {
                throw new BadRequestException(
                        "Phone number must include a valid country code (e.g. +91) and a 9 or 10-digit mobile number");
            }
            user.setPhoneNumber(phoneNumber.trim());
        }

        return UserResponse.from(userRepository.save(user));
    }

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

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(String query, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return PagedResponse.from(
                userRepository.searchUsers(query == null ? "" : query.trim(), pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserForAdmin(Long userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

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

    @Transactional
    public void deleteUser(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new BadRequestException("You cannot delete your own administrator account.");
        }

        User user = findUserOrThrow(userId);

        // 1. Delete user's cart and cart items if any
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);

        // 2. Delete user's saved addresses
        addressRepository.deleteAll(addressRepository.findByUserId(userId));

        // 3. Delete user's product reviews and recalculate affected product ratings
        List<ProductReview> reviews = productReviewRepository.findByUserId(userId);
        Set<Long> affectedProductIds = new HashSet<>();
        for (ProductReview review : reviews) {
            if (review.getProduct() != null) {
                affectedProductIds.add(review.getProduct().getId());
            }
        }
        productReviewRepository.deleteAll(reviews);
        productReviewRepository.flush();

        for (Long productId : affectedProductIds) {
            recalculateProductRating(productId);
        }

        // 4. Delete user's notifications
        notificationRepository.deleteAll(notificationRepository.findByUserId(userId));

        // 5. Delete user's orders (and cascade to order items & status history)
        orderRepository.deleteAll(orderRepository.findByUserId(userId));

        // 6. Delete user entity (removes user_roles and users table record, freeing up email)
        userRepository.delete(user);
        log.info("Admin {} permanently deleted user account {} ({})", adminId, userId, user.getEmail());
    }

    private void recalculateProductRating(Long productId) {
        Double average = productReviewRepository.calculateAverageRating(productId);
        long count = productReviewRepository.countByProductId(productId);

        productRepository.findById(productId).ifPresent(product -> {
            product.setAverageRating(average == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            product.setReviewCount((int) count);
            productRepository.save(product);
        });
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
