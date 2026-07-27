package com.giftwise.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResult {
    private UUID id;
    private String name;
    private UUID businessId;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private String occasion;
    private String ageGroup;
    private boolean active;
    private LocalDateTime createdAt;
}
