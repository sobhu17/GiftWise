package com.giftwise.auth.security;

import com.giftwise.shared.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Validate the request's bearer token (if present) and, on success, populate the
     * Spring Security context with the authenticated {@code Business}.
     * <p>
     * Runs once per request, before {@code SecurityConfig}'s authorization rules are evaluated.
     * A missing/invalid token is not rejected here — the filter always calls
     * {@code filterChain.doFilter}; it's {@code SecurityConfig}'s {@code authorizeHttpRequests}
     * rules that decide whether the (now possibly still-unauthenticated) request may proceed.
     * Unlike product-service's filter, this one loads the full {@code Business} via
     * {@code UserDetailsService} (keyed by the email in the token) so its roles are
     * available as authorities — auth-service is the one service that authenticates users.
     *
     * @param request     : the incoming HTTP request, inspected for an {@code Authorization} header
     * @param response    : the HTTP response, passed through untouched to the next filter
     * @param filterChain : the remaining filter chain, always invoked exactly once
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract Authorization header
        String authHeader = request.getHeader("Authorization");

        // 2. If no Bearer token, skip — SecurityConfig decides if endpoint needs auth
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token from "Bearer <token>"
        String token = authHeader.substring(7);

        // 4. Validate and set authentication in Spring Security context
        if (jwtService.validateToken(token)) {
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Build an authentication object and put it in the security context
            // This tells Spring Security: this request is authenticated as this user
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,               // credentials null — already authenticated via JWT
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 5. Always continue — SecurityConfig decides if the endpoint needs auth
        filterChain.doFilter(request, response);
    }
}
