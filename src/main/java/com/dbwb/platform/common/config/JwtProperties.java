package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT signing configuration (dbwb.jwt.*). Kept as a typed bean, like every
 * other environment-dependent value, rather than scattered {@code @Value}
 * injections per class.
 */
@Configuration
@ConfigurationProperties(prefix = "dbwb.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenTtlMinutes;
    private boolean refreshCookieSecure;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenTtlMinutes() {
        return accessTokenTtlMinutes;
    }

    public void setAccessTokenTtlMinutes(long accessTokenTtlMinutes) {
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    /** Whether the refresh-token cookie requires HTTPS - disabled only for local http:// development via REFRESH_COOKIE_SECURE=false. */
    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }
}
