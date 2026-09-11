package com.dbwb.platform.portfolio.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One line of an owner's work history on a PORTFOLIO website.
 *
 * A sibling of PortfolioProject rather than the same thing: a project is a
 * piece of work with a picture and links, an experience entry is a job with a
 * period and a title. Two of the portfolio templates render them as separate
 * sections, and did so from free-form section JSON until this table existed.
 *
 * Only the role is required. A line that says "Head of design" with no company
 * and no year is still worth showing, and the templates already hide the rest.
 */
@Entity
@Table(name = "experience_entries")
public class ExperienceEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @Column(nullable = false)
    private String role;

    private String company;

    /**
     * Free text: "2024", "2023-24", "2019 - Present". The column is entry_year
     * rather than year because H2 reserves YEAR as an identifier and the test
     * profile builds its schema there - see PortfolioProject.year for the time
     * that cost a table nobody noticed was missing.
     */
    @Column(name = "entry_year", length = 64)
    private String year;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
