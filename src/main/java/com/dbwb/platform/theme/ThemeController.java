package com.dbwb.platform.theme;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.theme.dto.ThemeResponse;
import com.dbwb.platform.theme.repository.ThemeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only: a Business Owner picks a theme during website creation (9.2/9.3).
 * Theme authoring (create/edit/assign to plans) is a Super Admin action -
 * see the admin module (BR-ADM-006), not exposed here.
 */
@RestController
@RequestMapping("/api/public/themes")
public class ThemeController {

    private final ThemeRepository themeRepository;

    public ThemeController(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @GetMapping
    public ApiResponse<List<ThemeResponse>> list() {
        return ApiResponse.ok(themeRepository.findByActiveTrue().stream().map(ThemeResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ThemeResponse> get(@PathVariable UUID id) {
        var theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theme not found."));
        return ApiResponse.ok(ThemeResponse.from(theme));
    }
}
