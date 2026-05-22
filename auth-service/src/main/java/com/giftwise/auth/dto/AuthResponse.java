package com.giftwise.auth.dto;

import com.giftwise.auth.model.Business;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String token;
    private UUID businessId;
    private String name;
    private String email;

    public static AuthResponse from(Business business, String token) {
        return AuthResponse.builder()
                .token(token)
                .businessId(business.getId())
                .name(business.getName())
                .email(business.getEmail())
                .build();
    }
}
