package com.giftwise.recommendation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftwise.recommendation.dto.ConversationTurn;
import com.giftwise.recommendation.dto.RecipientProfile;
import com.giftwise.recommendation.exception.ClaudeApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${giftwise.claude.api-key}")
    private String apiKey;

    @Value("${giftwise.claude.model}")
    private String model;

    @Value("${giftwise.claude.base-url}")
    private String baseUrl;

    private static final int MAX_TOKENS = 1000;

    private static final String SYSTEM_PROMPT = """
            You are a gift preference extraction assistant.
            Your job is to analyze a conversation between a user and a gift recommendation chatbot
            and extract structured gift preferences for the recipient.
            
            A partial profile from prior turns will be provided. Merge any new information from the
            latest conversation into it — do not discard previously extracted fields.
            
            Extract the following fields:
            - relationship: the buyer's relationship to the recipient (e.g. "mother", "colleague")
            - interests: an array of hobbies or interests mentioned for the recipient
            - occasion: the gift occasion (e.g. "birthday", "wedding")
            - ageGroup: the recipient's age group (e.g. "adult", "teen", "child")
            - minPrice: minimum budget as a decimal string (e.g. "50.00"), or null if not mentioned
            - maxPrice: maximum budget as a decimal string (e.g. "100.00"), or null if not mentioned
            
            Return ONLY a JSON object in exactly this format, with no preamble, explanation, or markdown:
            {
              "relationship": "mother",
              "interests": ["gardening", "cooking"],
              "occasion": "birthday",
              "ageGroup": null,
              "minPrice": null,
              "maxPrice": "100.00"
            }
            
            If a field has not been mentioned in the conversation, set it to null.
            For interests, return an empty array [] if none have been mentioned.
            """;

    /**
     * Turn the conversation so far into a merged {@link RecipientProfile} with one call to
     * Claude's Messages API.
     * <p>
     * The prior partial profile is folded into the system prompt (see
     * {@link #buildSystemPrompt}) rather than appended to {@code conversationHistory} as an
     * extra turn — the Messages API rejects two consecutive same-role messages, and the last
     * turn in {@code conversationHistory} is normally the end user's own message.
     *
     * @param conversationHistory : the turns exchanged so far this session, in order
     * @param currentProfile      : the profile extracted from prior turns, or {@code null} on
     *                              the session's first turn
     * @return the merged profile, combining {@code currentProfile} with anything new this
     * turn's conversation revealed
     * @throws ClaudeApiException if Claude returns an empty response, or a body that doesn't
     * parse into a {@link RecipientProfile}
     */
    public RecipientProfile extractProfile(List<ConversationTurn> conversationHistory,
                                           RecipientProfile currentProfile) {
        String prompt = buildSystemPrompt(currentProfile);

        List<ClaudeMessage> messages = toClaudeMessages(conversationHistory);

        ClaudeRequest requestBody = new ClaudeRequest(model, MAX_TOKENS, prompt, messages);

        ClaudeResponse response = webClient.post()
                .uri(baseUrl + "/messages")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .block();

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new ClaudeApiException("Empty response from Claude API");
        }

        return parseProfile(response.content().get(0).text());
    }

    private List<ClaudeMessage> toClaudeMessages(List<ConversationTurn> conversationHistory) {
        List<ClaudeMessage> messages = conversationHistory.stream()
                .map(turn -> new ClaudeMessage(
                        turn.getRole().name().toLowerCase(),
                        turn.getContent()
                ))
                .collect(java.util.stream.Collectors.toList());

        return messages;
    }

    /**
     * Build the system prompt for this call: the static extraction instructions plus the
     * current partial profile serialized as JSON, so Claude merges new information into it
     * instead of starting over each turn.
     * <p>
     * {@code currentProfile} defaults to an empty profile rather than staying {@code null} —
     * without this, the prompt would embed the literal string {@code "null"} on the session's
     * first turn instead of a real JSON object.
     *
     * @param currentProfile : the profile extracted from prior turns, or {@code null} on the
     *                         session's first turn
     * @return the full system prompt string for this call
     */
    private String buildSystemPrompt(RecipientProfile currentProfile) {
        if (currentProfile == null) {
            currentProfile = RecipientProfile.builder()
                    .interests(List.of())
                    .build();
        }

        return SYSTEM_PROMPT +
                "\nCurrent extracted profile from prior turns (merge new information into this, do not discard existing fields):\n" +
                writeJson(currentProfile);
    }

    private RecipientProfile parseProfile(String json) {
        try {
            return objectMapper.readValue(json, RecipientProfile.class);
        } catch (Exception e) {
            throw new ClaudeApiException("Failed to parse RecipientProfile from Claude response: " + e.getMessage());
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new ClaudeApiException("Failed to serialize object to JSON: " + e.getMessage());
        }
    }

    // ── Internal records for Claude API serialization/deserialization ──

    private record ClaudeRequest(
            String model,
            @com.fasterxml.jackson.annotation.JsonProperty("max_tokens") int maxTokens,
            String system,
            List<ClaudeMessage> messages
    ) {}

    private record ClaudeMessage(String role, String content) {}

    private record ClaudeResponse(List<ClaudeContent> content) {}

    private record ClaudeContent(String type, String text) {}
}