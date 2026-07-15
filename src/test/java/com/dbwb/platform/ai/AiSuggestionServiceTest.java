package com.dbwb.platform.ai;

import com.dbwb.platform.ai.dto.AiSuggestionFieldType;
import com.dbwb.platform.ai.dto.AiSuggestionRequest;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSuggestionServiceTest {

    @Test
    void refusesWithAClearMessageWhenNoApiKeyIsConfigured() {
        AiProperties properties = new AiProperties();
        properties.setOpenrouterApiKey("");
        AiSuggestionService service = new AiSuggestionService(properties);

        AiSuggestionRequest request = new AiSuggestionRequest("Test Cafe", "MENU_ORDERING", AiSuggestionFieldType.HERO_HEADING, null);

        assertThatThrownBy(() -> service.suggest(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("isn't configured");
    }
}
