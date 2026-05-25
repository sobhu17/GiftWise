package com.giftwise.auth.service;

import com.giftwise.auth.model.Business;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    @Value("${giftwise.jwt.secret}")
    private String jwtSecret;

    @Value("${giftwise.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

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
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractBusinessId(String token) {
        return getClaims(token).get("businessId", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
