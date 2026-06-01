package com.giftwise.auth.contoller;

import com.giftwise.auth.dto.ApiKeyListResponse;
import com.giftwise.auth.dto.ApiKeyResponse;
import com.giftwise.auth.model.Business;
import com.giftwise.auth.service.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiKeyResponse> generateApiKey() {
        Business business = getAuthenticatedBusiness();
        ApiKeyResponse response = apiKeyService.generateApiKey(business.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyListResponse>> listApiKeys() {
        Business business = getAuthenticatedBusiness();
        List<ApiKeyListResponse> response = apiKeyService.listApiKeys(business.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID keyId) {
        Business business = getAuthenticatedBusiness();
        apiKeyService.revokeApiKey(keyId, business.getId());
        return ResponseEntity.noContent().build();
    }

    private Business getAuthenticatedBusiness() {
        return (Business) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
