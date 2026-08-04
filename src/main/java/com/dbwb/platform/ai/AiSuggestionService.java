package com.dbwb.platform.ai;

import com.dbwb.platform.ai.dto.AiSuggestionFieldType;
import com.dbwb.platform.ai.dto.AiSuggestionRequest;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Generates short pieces of website copy via OpenRouter (default model:
 * DeepSeek). The model is a config value (dbwb.ai.model), not hardcoded, so
 * it can be swapped for any other OpenRouter-hosted model without a code
 * change - see AiProperties.
 */
@Service
public class AiSuggestionService {

    private final AiProperties properties;
    private final RestClient restClient;

    public AiSuggestionService(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl("https://openrouter.ai/api/v1").build();
    }

    public String suggest(AiSuggestionRequest request) {
        if (properties.getOpenrouterApiKey() == null || properties.getOpenrouterApiKey().isBlank()) {
            throw new BusinessRuleViolationException("The AI assistant isn't configured on this server yet.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of("role", "user", "content", buildPrompt(request))));

        try {
            OpenRouterChatResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.getOpenrouterApiKey())
                    .body(body)
                    .retrieve()
                    .body(OpenRouterChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new BusinessRuleViolationException("The AI assistant didn't return a suggestion. Please try again.");
            }
            return response.choices().get(0).message().content().trim().replaceAll("^\"|\"$", "");
        } catch (RestClientException e) {
            throw new BusinessRuleViolationException("The AI assistant is temporarily unavailable. Please try again.");
        }
    }

    private String buildPrompt(AiSuggestionRequest request) {
        String kind = describeFieldType(request.fieldType());
        StringBuilder prompt = new StringBuilder("Write ").append(kind)
                .append(" for a business called \"").append(request.businessName()).append("\"");

        if ("PORTFOLIO".equals(request.templateType())) {
            prompt.append(", which showcases services on a portfolio-style website");
        } else if ("MENU_ORDERING".equals(request.templateType())) {
            prompt.append(", which runs a menu/ordering website");
        }
        if (request.existingText() != null && !request.existingText().isBlank()) {
            prompt.append(". Improve on or take inspiration from this existing draft: \"").append(request.existingText()).append("\"");
        }
        prompt.append(". Reply with ONLY the suggested text itself - no quotes, no markdown, no explanation.");
        return prompt.toString();
    }

    private String describeFieldType(AiSuggestionFieldType fieldType) {
        return switch (fieldType) {
            case HERO_HEADING -> "a short, catchy tagline (5-8 words) for the hero section of its website";
            case HERO_SUBTITLE -> "a one-sentence supporting line (20 words max) for the hero section of its website";
            case BUSINESS_DESCRIPTION -> "a warm, 2-3 sentence description for the About section of its website";
            case SEO_META_TITLE -> "an SEO meta title (60 characters max)";
            case SEO_META_DESCRIPTION -> "an SEO meta description (155 characters max)";
        };
    }

    private record OpenRouterChatResponse(List<Choice> choices) {
        private record Choice(Message message) {
        }

        private record Message(String content) {
        }
    }
}
