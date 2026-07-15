package com.dbwb.platform.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Image upload storage config (dbwb.uploads.*) - local disk today, swappable per environment. */
@Configuration
@ConfigurationProperties(prefix = "dbwb.uploads")
public class UploadProperties {

    private String directory = "uploads";
    private int maxFileSizeMb = 5;

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }
}
