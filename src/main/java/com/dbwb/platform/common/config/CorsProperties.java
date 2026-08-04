package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** CORS configuration (dbwb.cors.*) - the allowed origin list, per environment. */
@Configuration
@ConfigurationProperties(prefix = "dbwb.cors")
public class CorsProperties {

    private String allowedOrigins;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOriginsList() {
        return List.of(allowedOrigins.split(","));
    }
}
