package com.dbwb.platform.website.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** BRD 9.11: per-website SEO metadata for the public site's <head> and share previews. */
@Entity
@Table(name = "seo_metadata")
public class SeoMetadata extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false, unique = true)
    private BusinessWebsite website;

    @Column(length = 70)
    private String metaTitle;

    @Column(length = 160)
    private String metaDescription;

    private String ogImageUrl;

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getOgImageUrl() {
        return ogImageUrl;
    }

    public void setOgImageUrl(String ogImageUrl) {
        this.ogImageUrl = ogImageUrl;
    }
}
