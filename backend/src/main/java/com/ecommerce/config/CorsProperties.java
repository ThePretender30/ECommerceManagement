package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Allowed browser origins, bound from {@code app.cors.allowed-origins}.
 *
 * <p>Kept as an explicit list rather than a wildcard: credentialed requests (the ones
 * carrying our JWT) cannot legally use {@code *}, and an explicit list means a deployment
 * only ever accepts the frontends it actually owns.
 */
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:5173");
}
