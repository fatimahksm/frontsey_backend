package com.dbwb.platform.theme;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.theme.dto.ThemeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThemeConfigValidatorTest {

    private ThemeConfigValidator validator;

    @BeforeEach
    void setUp() {
        Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
        validator = new ThemeConfigValidator(new ObjectMapper(), beanValidator);
    }

    private static final String VALID_JSON = """
            {
              "fontFamily": "SYSTEM_SANS",
              "headingFontFamily": "ELEGANT_SERIF",
              "primaryColor": "#7c3aed",
              "secondaryColor": "#f4f0fb",
              "backgroundColor": "#ffffff",
              "surfaceColor": "#ffffff",
              "textColor": "#0a0a0f",
              "buttonStyle": "PILL",
              "cardStyle": "SOFT_SHADOW",
              "borderRadius": 16,
              "sectionSpacing": "COMFORTABLE"
            }
            """;

    @Test
    void parsesAWellFormedConfig() {
        ThemeConfig config = validator.parseAndValidate(VALID_JSON);

        assertThat(config.primaryColor()).isEqualTo("#7c3aed");
        assertThat(config.buttonStyle()).isEqualTo(ThemeConfig.ButtonStyle.PILL);
        assertThat(config.borderRadius()).isEqualTo(16);
    }

    @Test
    void rejectsBlankConfig() {
        assertThatThrownBy(() -> validator.parseAndValidate(""))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> validator.parseAndValidate(null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> validator.parseAndValidate("{not valid json"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsAColorThatIsNotAHexTriplet() {
        String badColor = VALID_JSON.replace("\"#7c3aed\"", "\"purple\"");

        assertThatThrownBy(() -> validator.parseAndValidate(badColor))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("primaryColor");
    }

    @Test
    void rejectsBorderRadiusOutsideZeroToThirtyTwo() {
        String tooLarge = VALID_JSON.replace("\"borderRadius\": 16", "\"borderRadius\": 999");

        assertThatThrownBy(() -> validator.parseAndValidate(tooLarge))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("borderRadius");
    }

    @Test
    void rejectsAnUnknownEnumValue() {
        String badEnum = VALID_JSON.replace("\"PILL\"", "\"BLOBBY\"");

        assertThatThrownBy(() -> validator.parseAndValidate(badEnum))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void parseOrDefaultFallsBackInsteadOfThrowing() {
        ThemeConfig config = validator.parseOrDefault("not json at all");

        assertThat(config).isEqualTo(ThemeConfig.defaults());
    }

    @Test
    void parseOrDefaultReturnsTheRealConfigWhenValid() {
        ThemeConfig config = validator.parseOrDefault(VALID_JSON);

        assertThat(config.primaryColor()).isEqualTo("#7c3aed");
    }
}
