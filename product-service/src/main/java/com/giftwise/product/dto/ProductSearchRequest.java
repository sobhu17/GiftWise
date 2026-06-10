package com.giftwise.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    @NotBlank(message = "Search query is required")
    private String query;
    private String category;
    private String occasion;
    private String ageGroup;
    @DecimalMin(value = "0.01", message = "Min price must be greater than 0")
    private BigDecimal minPrice;
    @DecimalMin(value = "0.01", message = "Max price must be greater than 0")
    private BigDecimal maxPrice;
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private int limit=10;
}
