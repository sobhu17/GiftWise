package com.giftwise.auth.dto;

import com.giftwise.auth.model.ApiKey;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ApiKeyResponse {
    private UUID keyId;
    private String fullKey;
    private String keyPrefix;

    public static ApiKeyResponse from(ApiKey apiKey, String fullKey) {
        return ApiKeyResponse.builder()
                .keyId(apiKey.getId())
                .fullKey(fullKey)
                .keyPrefix(apiKey.getKeyPrefix())
                .build();
    }
}
