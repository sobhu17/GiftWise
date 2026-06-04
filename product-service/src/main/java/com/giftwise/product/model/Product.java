package com.giftwise.product.model;

import com.giftwise.product.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    @Column(name = "description", nullable = false)
    private String description;
    @Column(name = "price", nullable = false)
    private BigDecimal price;
    @Column(name = "image_url", nullable = false)
    private String imageUrl;
    @Column(name = "category", nullable = false)
    private String category;
    @Column(name = "occasion", nullable = false)
    private String occasion;
    @Column(name = "age_group", nullable = false)
    private String ageGroup;
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding;
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

}
