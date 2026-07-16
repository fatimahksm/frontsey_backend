package com.dbwb.platform.sections.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An owner-added extra section on a website's page, on top of the fixed
 * template content. The `data` column is deliberately opaque JSON (like
 * BusinessWebsite.draftContent) since its shape depends entirely on `type` -
 * validated/structured on the frontend, not modelled per-type here.
 */
@Entity
@Table(name = "page_sections")
public class PageSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageSectionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(nullable = false)
    private int sortOrder = 0;

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public PageSectionType getType() {
        return type;
    }

    public void setType(PageSectionType type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
