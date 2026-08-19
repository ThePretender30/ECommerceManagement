package com.ecommerce.repository;

import com.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Case-insensitive search across name, email and phone, for the admin user list. */
    @Query("""
            SELECT u FROM User u
            WHERE :q IS NULL OR :q = ''
               OR LOWER(u.fullName)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email)      LIKE LOWER(CONCAT('%', :q, '%'))
               OR u.phoneNumber       LIKE CONCAT('%', :q, '%')
            """)
    Page<User> searchUsers(@Param("q") String q, Pageable pageable);

    long countByCreatedAtAfter(Instant since);
}
