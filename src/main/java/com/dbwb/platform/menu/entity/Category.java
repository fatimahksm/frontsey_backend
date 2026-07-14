package com.dbwb.platform.menu.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * BR-MENU-001: categories are scoped to one website. BR-MENU-008: ordering
 * is by creation date only in the MVP (BaseEntity.createdAt), no manual
 * reordering field is modeled.
 */
@Entity
@Table(name = "menu_categories")
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @Column(nullable = false)
    private String name;

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
}
