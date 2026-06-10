package com.giftwise.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Define auth-service's HTTP security rules: stateless JWT auth, CSRF disabled,
     * and which endpoints require an authenticated business.
     * <p>
     * {@code /auth/register}, {@code /auth/login}, and {@code /actuator/health} are public —
     * everything else (e.g. {@code /api-keys/**}) requires a valid bearer token, validated by
     * {@link JwtAuthFilter} which runs ahead of Spring's default username/password filter
     * in the chain.
     *
     * @param http : Spring Security's HTTP configuration builder
     * @return the assembled filter chain enforcing the rules above
     * @throws Exception if the security configuration fails to build
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use JWT, not session cookies, so CSRF doesn't apply
                .csrf(csrf -> csrf.disable())

                // Define which endpoints are public vs protected
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )

                // Stateless — no HTTP session, every request carries its own JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Add our JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
