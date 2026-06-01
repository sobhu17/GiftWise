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
    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);

    List<ApiKey> findAllByBusiness(Business business);
}
