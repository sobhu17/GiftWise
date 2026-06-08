package com.giftwise.product.security;

import com.giftwise.shared.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    /**
     * Validate the request's bearer token (if present) and, on success, populate the
     * Spring Security context with the business id extracted from its claims.
     * <p>
     * Runs once per request, before {@code SecurityConfig}'s authorization rules are evaluated.
     * A missing/invalid token is not rejected here — the filter always calls
     * {@code filterChain.doFilter}; it's {@code SecurityConfig}'s {@code authorizeHttpRequests}
     * rules that decide whether the (now possibly still-unauthenticated) request may proceed.
     * The {@code businessId} string becomes the authentication principal — there's no
     * {@code UserDetails}/{@code Business} entity here, product-service only needs the id.
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
            String businessId = jwtService.extractBusinessId(token);

            // Build an authentication object and put it in the security context
            // This tells Spring Security: this request is authenticated with this businessId
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            businessId,
                            null,               // credentials null — already authenticated via JWT
                            List.of()
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

