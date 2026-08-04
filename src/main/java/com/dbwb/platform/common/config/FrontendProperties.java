package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Public frontend configuration (dbwb.frontend.*) needed by backend-generated links (verification, reset, etc). */
@Configuration
@ConfigurationProperties(prefix = "dbwb.frontend")
public class FrontendProperties {

    private String publicSiteBaseUrl;

    public String getPublicSiteBaseUrl() {
        return publicSiteBaseUrl;
    }

    public void setPublicSiteBaseUrl(String publicSiteBaseUrl) {
        this.publicSiteBaseUrl = publicSiteBaseUrl;
    }
}
