package com.giftwise.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientProfile {
    private String relationship;
    private List<String> interests;
    private String occasion;
    private String ageGroup;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
