package com.giftwise.auth.service;

import com.giftwise.auth.model.Business;
import com.giftwise.shared.security.JwtService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;

    @Value("${giftwise.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Generate a signed JWT for a business, embedding its id, name, and roles as claims so
     * downstream services can authorize requests without a database lookup.
     *
     * @param business : the authenticated business to issue a token for
     * @return a signed JWT, valid for {@code giftwise.jwt.expiration-ms} milliseconds
     */
    public String generateToken(Business business) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("businessId", business.getId().toString());
        claims.put("businessName", business.getName());
        claims.put("roles", business.getRoles().stream()
                .map(role -> role.getName())
                .toList());


        return Jwts.builder()
                .issuer("GiftWise")
                .subject(business.getEmail())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(jwtService.getSigningKey())
                .compact();
    }
}
