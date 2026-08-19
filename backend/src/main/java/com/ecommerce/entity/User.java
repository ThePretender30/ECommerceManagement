package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A registered account. Both customers and admins live in this table; what they are
 * allowed to do is decided entirely by {@link #roles}.
 *
 * <p>{@code password} always holds a BCrypt hash - the plain text never reaches this class.
 * {@code phoneNumber} is the destination for WhatsApp order notifications and is stored in
 * E.164 form (e.g. {@code +919876543210}) because that is what Twilio requires.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 180)
    private String email;

    /** BCrypt hash. Never serialized to any DTO. */
    @Column(nullable = false, length = 100)
    private String password;

    /** E.164 format, used as the WhatsApp recipient. */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * EAGER because every authenticated request needs the authorities to build the
     * SecurityContext - a lazy collection here would mean an extra query per request
     * or a LazyInitializationException outside the transaction.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_roles_role"))
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }
}
