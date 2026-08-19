package com.ecommerce.dto.auth;

import com.ecommerce.entity.RoleName;
import com.ecommerce.entity.User;

import java.time.Instant;
import java.util.List;

/**
 * A safe public view of an account.
 *
 * <p>The password hash is structurally absent - it is not a field that could be
 * accidentally exposed by a serialization setting. Returning DTOs instead of entities is
 * what makes that guarantee possible.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        List<String> roles,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }

    public boolean isAdmin() {
        return roles.contains(RoleName.ROLE_ADMIN.name());
    }
}
