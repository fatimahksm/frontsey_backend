package com.dbwb.platform.theme.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * BR-THEME-008 / BR-ADM-006: Super Admin adds, edits, deletes, activates,
 * disables, and assigns themes to plans. Actual layout/section data for a
 * theme is expected to live in a structured JSON/config blob (themeConfig)
 * so new themes don't require code changes (BR-NFR-009).
 */
@Entity
@Table(name = "themes")
public class Theme extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    /** JSON describing supported sections/pages, default colors, fonts, etc. */
    @Column(columnDefinition = "TEXT")
    private String themeConfig;

    private boolean active = true;

    // --- getters / setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThemeConfig() {
        return themeConfig;
    }

    public void setThemeConfig(String themeConfig) {
        this.themeConfig = themeConfig;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
