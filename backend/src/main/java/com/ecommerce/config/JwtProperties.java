package com.ecommerce.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank
    private String secret;

    private long expirationMs = 86_400_000L;

    private String issuer = "ecommerce-api";

    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("""

                    ============================================================
                     APP_JWT_SECRET is not set.

                     Create backend/.env (copy from .env.example) and set:
                       APP_JWT_SECRET=<at least 32 random characters>

                     Then start the app with:  .\\run.ps1
                    ============================================================
                    """);
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "APP_JWT_SECRET must be at least 32 characters for HS256 (currently %d)."
                            .formatted(secret.length()));
        }
    }
}
