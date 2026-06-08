package com.giftwise.product.dto;

import com.giftwise.product.model.Product;
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
public class ProductResponse {
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

    /**
     * Map a {@link Product} entity to its REST response shape.
     * <p>
     * Deliberately omits {@code embedding} — sending 1536 floats over REST to a business
     * dashboard would be wasteful and the field is meaningless to API consumers; it only
     * matters internally for vector search.
     *
     * @param product : the persisted entity to convert
     * @return a response DTO containing every client-facing field of {@code product}
     */
    public  static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .businessId(product.getBusinessId())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .occasion(product.getOccasion())
                .ageGroup(product.getAgeGroup())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
