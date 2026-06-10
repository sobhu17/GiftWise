package com.giftwise.auth.repository;

import com.giftwise.auth.model.ApiKey;
import com.giftwise.auth.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Look up an active API key by its hash — the gRPC {@code ValidateApiKey} path hashes
     * the raw key it receives and looks it up here, since the raw key itself is never stored.
     * Revoked keys ({@code isActive = false}) never match, even if the hash exists.
     *
     * @param keyHash : SHA-256 hash of the raw API key
     * @return the matching active key, or empty if no active key has this hash
     */
    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);

    /**
     * List every API key belonging to a business, including revoked ones — feeds the
     * key management dashboard.
     *
     * @param business : the business whose keys to list
     * @return all API keys belonging to {@code business}
     */
    List<ApiKey> findAllByBusiness(Business business);
}
