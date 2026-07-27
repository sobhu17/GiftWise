package com.giftwise.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {
    private UUID sessionId;
    private UUID businessId;
    private List<ConversationTurn> conversationHistory;
    private RecipientProfile recipientProfile;
}
