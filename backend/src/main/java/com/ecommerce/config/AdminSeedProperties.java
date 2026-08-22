package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
@Getter
@Setter
public class AdminSeedProperties {

    private String email = "admin@ecommerce.local";
    private String password;
    private String fullName = "System Administrator";
    private String phoneNumber = "+910000000000";
}
