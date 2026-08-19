package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The bootstrap admin account, bound from {@code app.admin.*}.
 *
 * <p>Used once by {@code DataSeeder} on an empty database. The password comes from the
 * {@code ADMIN_PASSWORD} environment variable and is BCrypt-hashed before it is stored -
 * it is never written to the database or to a config file in plain text.
 */
@ConfigurationProperties(prefix = "app.admin")
@Getter
@Setter
public class AdminSeedProperties {

    private String email = "admin@ecommerce.local";

    /** Required on first run only; seeding is skipped with a warning if absent. */
    private String password;

    private String fullName = "System Administrator";

    private String phoneNumber = "+910000000000";
}
