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
