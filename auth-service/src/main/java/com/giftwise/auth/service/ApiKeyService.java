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

    /**
     * Generate a new API key for a business, returning the full key exactly once.
     * <p>
     * Only the SHA-256 hash and a short prefix are persisted — the raw key is never stored,
     * so it cannot be recovered later. The caller must save it now.
     *
     * @param businessId : id of the business the key belongs to
     * @return the new key's metadata along with the full plaintext key
     * @throws RuntimeException if no business exists with this id
     */
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

    /**
     * List every API key belonging to a business, including revoked ones, for the
     * key management dashboard.
     *
     * @param businessId : id of the business whose keys to list
     * @return all API keys for this business, mapped to response DTOs (no key hashes exposed)
     * @throws RuntimeException if no business exists with this id
     */
    public List<ApiKeyListResponse> listApiKeys(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        return apiKeyRepository.findAllByBusiness(business)
                .stream()
                .map(ApiKeyListResponse::from)
                .toList();
    }

    /**
     * Revoke an API key by setting {@code isActive} to false.
     * <p>
     * Ownership is checked explicitly here rather than via a scoped repository query, because
     * the lookup needs to distinguish between "key not found" and "key belongs to someone else".
     *
     * @param keyId      : id of the key to revoke
     * @param businessId : id of the authenticated business, used as an ownership check
     * @throws RuntimeException if no business or key exists with these ids, or if the key
     *                           does not belong to this business
     */
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

    /**
     * Record that an active API key was just used, for the key management dashboard's
     * "last used" display.
     * <p>
     * Looked up by hash since this is called from the gRPC {@code ValidateApiKey} path, which
     * only ever has the raw key (and therefore its hash), never the key's id.
     *
     * @param keyHash : SHA-256 hash of the API key that was used
     * @throws RuntimeException if no active key exists with this hash
     */
    public void updateLastUsedAt(String keyHash) {
        ApiKey apiKey = apiKeyRepository.findByKeyHashAndIsActiveTrue(keyHash)
                .orElseThrow(() -> new RuntimeException("API Key not found"));
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
    }

}
