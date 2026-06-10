package com.giftwise.auth.controller;

import com.giftwise.auth.dto.ApiKeyListResponse;
import com.giftwise.auth.dto.ApiKeyResponse;
import com.giftwise.auth.model.Business;
import com.giftwise.auth.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * Generate a new API key for the authenticated business.
     * <p>
     * The full key is returned only in this response — only its hash and prefix are
     * stored, so it cannot be retrieved again later.
     *
     * @return 200 OK with the new key's metadata and the full plaintext key
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> generateApiKey() {
        Business business = getAuthenticatedBusiness();
        ApiKeyResponse response = apiKeyService.generateApiKey(business.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * List every API key belonging to the authenticated business, including revoked ones.
     *
     * @return 200 OK with the full list of keys, showing only their prefixes (not full keys)
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyListResponse>> listApiKeys() {
        Business business = getAuthenticatedBusiness();
        List<ApiKeyListResponse> response = apiKeyService.listApiKeys(business.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke an API key belonging to the authenticated business.
     *
     * @param keyId : id of the key to revoke, taken from the path
     * @return 204 No Content on success
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID keyId) {
        Business business = getAuthenticatedBusiness();
        apiKeyService.revokeApiKey(keyId, business.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Read the {@link Business} that {@link com.giftwise.auth.security.JwtAuthFilter} placed in
     * the security context as the authentication principal.
     *
     * @return the business that owns the current authenticated request
     */
    private Business getAuthenticatedBusiness() {
        return (Business) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
