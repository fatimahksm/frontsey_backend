package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Optional first-Super-Admin bootstrap (dbwb.admin.bootstrap-*). Registration
 * always creates a Business Owner account (BR-AUTH-001), so without this
 * there would be no way to ever create the platform's first Super Admin.
 * Left blank, bootstrap is skipped entirely.
 */
@Configuration
@ConfigurationProperties(prefix = "dbwb.admin")
public class AdminBootstrapProperties {

    private String bootstrapEmail;
    private String bootstrapPassword;

    public String getBootstrapEmail() {
        return bootstrapEmail;
    }

    public void setBootstrapEmail(String bootstrapEmail) {
        this.bootstrapEmail = bootstrapEmail;
    }

    public String getBootstrapPassword() {
        return bootstrapPassword;
    }

    public void setBootstrapPassword(String bootstrapPassword) {
        this.bootstrapPassword = bootstrapPassword;
    }
}
