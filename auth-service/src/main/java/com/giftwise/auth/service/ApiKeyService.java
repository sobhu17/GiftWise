package com.giftwise.auth.service;

import com.giftwise.auth.dto.ApiKeyListResponse;
import com.giftwise.auth.dto.ApiKeyResponse;
import com.giftwise.auth.model.ApiKey;
import com.giftwise.auth.model.Business;
import com.giftwise.auth.repository.ApiKeyRepository;
import com.giftwise.auth.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    private final BusinessRepository businessRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyResponse generateApiKey(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        String rawKey = "gw_live" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        String keyHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes());
            keyHash = HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(rawKey.substring(0 , 12));
        apiKey.setActive(true);
        apiKey.setBusiness(business);

        ApiKey response = apiKeyRepository.save(apiKey);
        return ApiKeyResponse.from(response, rawKey);
    }

    public List<ApiKeyListResponse> listApiKeys(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        return apiKeyRepository.findAllByBusiness(business)
                .stream()
                .map(ApiKeyListResponse::from)
                .toList();
    }

    public void revokeApiKey(UUID keyId , UUID businessId){
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new RuntimeException("API Key not found"));

        if (!apiKey.getBusiness().getId().equals(businessId)) {
            throw new RuntimeException("Unauthorized - this key does not belong to your business");
        }

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }

    public void updateLastUsedAt(String keyHash) {
        ApiKey apiKey = apiKeyRepository.findByKeyHashAndIsActiveTrue(keyHash)
                .orElseThrow(() -> new RuntimeException("API Key not found"));
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
    }

}
