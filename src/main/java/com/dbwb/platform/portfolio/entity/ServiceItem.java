package com.dbwb.platform.portfolio.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A single offering on a PORTFOLIO-template website - deliberately much
 * simpler than MenuItem (no sizes/add-ons/box variants/availability
 * windows/discount), since portfolio businesses (salons, studios,
 * agencies...) don't need per-item cart configuration the way a
 * WhatsApp-ordering menu does. Named ServiceItem (not "Service") to avoid
 * clashing with the org.springframework.stereotype.Service annotation used
 * throughout this codebase's own *Service classes.
 */
@Entity
@Table(name = "services")
public class ServiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Optional - some services are priced "on request" rather than fixed. */
    private BigDecimal price;

    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder = 0;

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
