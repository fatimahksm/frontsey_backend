package com.dbwb.platform.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** AI content-suggestion assistant config (dbwb.ai.*) - OpenRouter today, swappable via `model` with no code change. */
@Configuration
@ConfigurationProperties(prefix = "dbwb.ai")
public class AiProperties {

    private String openrouterApiKey = "";
    private String model = "deepseek/deepseek-chat";

    public String getOpenrouterApiKey() {
        return openrouterApiKey;
    }

    public void setOpenrouterApiKey(String openrouterApiKey) {
        this.openrouterApiKey = openrouterApiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
