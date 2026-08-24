package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Request-rate ceilings (dbwb.rate-limits.*), one policy per endpoint worth
 * abusing. Like every other business number on this platform these live in
 * configuration rather than in the code enforcing them, so tightening a limit
 * after an incident is a config change.
 */
@Configuration
@ConfigurationProperties(prefix = "dbwb.rate-limits")
public class RateLimitProperties {

    /** Master switch, so a test can assert on behaviour without tripping a ceiling. */
    private boolean enabled = true;

    /** Per IP. The one that matters most: unbounded login is an open invitation to credential stuffing. */
    private Policy login = new Policy(10, 15);

    /** Per IP. Slower than login - nobody legitimately opens accounts in bulk. */
    private Policy registration = new Policy(5, 60);

    /** Per IP. Also caps how much mail an attacker can have sent to someone else's inbox. */
    private Policy passwordReset = new Policy(5, 60);

    /** Per account. Each call spends real money at OpenRouter, so this is a billing control as much as an abuse one. */
    private Policy aiSuggestions = new Policy(30, 60);

    /** Per IP. Public and unauthenticated: without a ceiling anyone can inflate a site's numbers and flood analytics_events. */
    private Policy publicItemView = new Policy(60, 1);

    /**
     * Per IP. Loading a public page also records a visit, so the same flood is
     * possible through the page itself. Set well above real browsing - a shared
     * office or cafe NAT must never hit it - while still capping a script.
     */
    private Policy publicPageView = new Policy(240, 1);

    public static class Policy {
        /** Requests permitted per window. */
        private int limit;
        /** Length of the window, in minutes. */
        private int windowMinutes;

        public Policy() {
        }

        public Policy(int limit, int windowMinutes) {
            this.limit = limit;
            this.windowMinutes = windowMinutes;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(int windowMinutes) {
            this.windowMinutes = windowMinutes;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Policy getLogin() {
        return login;
    }

    public void setLogin(Policy login) {
        this.login = login;
    }

    public Policy getRegistration() {
        return registration;
    }

    public void setRegistration(Policy registration) {
        this.registration = registration;
    }

    public Policy getPasswordReset() {
        return passwordReset;
    }

    public void setPasswordReset(Policy passwordReset) {
        this.passwordReset = passwordReset;
    }

    public Policy getAiSuggestions() {
        return aiSuggestions;
    }

    public void setAiSuggestions(Policy aiSuggestions) {
        this.aiSuggestions = aiSuggestions;
    }

    public Policy getPublicItemView() {
        return publicItemView;
    }

    public void setPublicItemView(Policy publicItemView) {
        this.publicItemView = publicItemView;
    }

    public Policy getPublicPageView() {
        return publicPageView;
    }

    public void setPublicPageView(Policy publicPageView) {
        this.publicPageView = publicPageView;
    }
}
