package com.dbwb.platform.ai;

import com.dbwb.platform.ai.dto.AiSuggestionRequest;
import com.dbwb.platform.ai.dto.AiSuggestionResponse;
import com.dbwb.platform.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Requires authentication (falls under SecurityConfig's default anyRequest().authenticated()) - keeps this from being an open, unmetered proxy to a paid API. */
@RestController
@RequestMapping("/api/ai")
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    public AiSuggestionController(AiSuggestionService aiSuggestionService) {
        this.aiSuggestionService = aiSuggestionService;
    }

    @PostMapping("/suggestions")
    public ApiResponse<AiSuggestionResponse> suggest(@Valid @RequestBody AiSuggestionRequest request) {
        return ApiResponse.ok(new AiSuggestionResponse(aiSuggestionService.suggest(request)));
    }
}
