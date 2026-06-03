package com.giftwise.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Description is required")
    private String description;
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    @NotBlank(message = "Category is required")
    private String category;
    @NotBlank(message = "Occasion is required")
    private String occasion;
    @NotBlank(message = "Age Group is required")
    private String ageGroup;
}
