package com.dbwb.platform.theme;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.theme.dto.ThemeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses and validates a Theme's raw {@code themeConfig} JSON against the
 * strongly-typed {@link ThemeConfig} schema. Used both when a Super Admin
 * authors/edits a theme (reject invalid config up front, at the point of
 * write) and when the public renderer needs a website's effective theme
 * (defensive re-validation, since the column is still a free-text TEXT
 * field at the database level).
 */
@Component
public class ThemeConfigValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ThemeConfigValidator(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /** @throws BusinessRuleViolationException if the JSON is malformed or fails any constraint. */
    public ThemeConfig parseAndValidate(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new BusinessRuleViolationException("Theme configuration is required.");
        }
        ThemeConfig config;
        try {
            config = objectMapper.readValue(rawJson, ThemeConfig.class);
        } catch (Exception e) {
            throw new BusinessRuleViolationException("Theme configuration is not valid JSON for the expected schema.");
        }
        Set<ConstraintViolation<ThemeConfig>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new BusinessRuleViolationException("Invalid theme configuration: " + message);
        }
        return config;
    }

    /** Falls back to {@link ThemeConfig#defaults()} on malformed data instead of failing a page render for visitors. */
    public ThemeConfig parseOrDefault(String rawJson) {
        try {
            return parseAndValidate(rawJson);
        } catch (BusinessRuleViolationException e) {
            return ThemeConfig.defaults();
        }
    }
}
