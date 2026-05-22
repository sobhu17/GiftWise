package com.giftwise.auth.dto;

import com.giftwise.auth.model.ApiKey;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApiKeyListResponse {
    private UUID keyId;
    private String keyPrefix;
    private boolean isActive;
    private LocalDateTime lastUsedAt;

    public static ApiKeyListResponse from(ApiKey apiKey) {
        return ApiKeyListResponse.builder()
                .keyId(apiKey.getId())
                .keyPrefix(apiKey.getKeyPrefix())
                .isActive(apiKey.isActive())
                .lastUsedAt(apiKey.getLastUsedAt())
                .build();
    }

}
