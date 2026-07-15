package com.dbwb.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Support/contact-form configuration (dbwb.support.*). TBD-012: final categories/limits are a pending business decision. */
@Configuration
@ConfigurationProperties(prefix = "dbwb.support")
public class SupportProperties {

    private String notificationEmail;

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }
}
