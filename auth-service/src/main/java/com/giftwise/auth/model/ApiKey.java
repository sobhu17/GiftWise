package com.giftwise.auth.model;

import com.giftwise.auth.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;
    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
}
