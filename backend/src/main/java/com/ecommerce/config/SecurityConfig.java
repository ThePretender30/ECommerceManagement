package com.ecommerce.config;

import com.ecommerce.security.JwtAuthEntryPoint;
import com.ecommerce.security.JwtAuthenticationFilter;
import com.ecommerce.security.RestAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The application's authorisation rules and filter chain.
 *
 * <p>Three things are worth understanding here:
 * <ul>
 *   <li><b>Stateless.</b> No HTTP session is ever created; identity comes from the JWT on
 *       each request. This is also why CSRF protection is disabled - there is no session
 *       cookie for an attacker to ride on.</li>
 *   <li><b>Read is public, write is not.</b> Anonymous visitors can browse and search
 *       products and read reviews, because a storefront that demands a login to look at a
 *       catalogue is useless. Everything that touches a specific person's data or changes
 *       state requires authentication.</li>
 *   <li><b>{@code /api/admin/**} is gated in one place.</b> A single rule covers every
 *       current and future admin endpoint, so a new admin controller cannot accidentally
 *       ship unprotected.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize for the few finer-grained checks
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    /** BCrypt automatically salts each hash, so identical passwords store differently. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Safe to disable: we authenticate with a bearer token, not a cookie.
            .csrf(AbstractHttpConfigurer -> AbstractHttpConfigurer.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthEntryPoint)   // 401 as JSON
                    .accessDeniedHandler(accessDeniedHandler))     // 403 as JSON
            .authorizeHttpRequests(auth -> auth

                    // --- Public: authentication endpoints -------------------
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                    // --- Public: browsing the storefront --------------------
                    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                    // --- Public: API documentation --------------------------
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()

                    // Browsers send a credential-less OPTIONS preflight first.
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // --- Admin only -----------------------------------------
                    // One rule covering every admin endpoint, present and future.
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // --- Everything else needs a valid token ----------------
                    // Writing a review, the cart, orders, addresses, profile.
                    .anyRequest().authenticated()
            )
            // Our filter must run before the username/password filter so the
            // SecurityContext is already populated when authorisation is evaluated.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS for the React dev server and any configured production origin.
     *
     * <p>{@code allowCredentials(true)} forbids a {@code *} origin by specification, which
     * is why {@link CorsProperties} holds an explicit list.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
