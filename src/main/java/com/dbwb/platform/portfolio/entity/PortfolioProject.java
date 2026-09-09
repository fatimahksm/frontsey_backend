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
 * One project on a PORTFOLIO website.
 *
 * Separate from ServiceItem because the two answer different questions: a
 * service is what the owner sells, a project is what they already did. The
 * rebuilt Portfolio templates lead with the second, and need a name,
 * discipline and links per entry rather than a price.
 *
 * Everything but the name is optional - a picture and a title is a complete
 * portfolio entry, and the templates hide whatever is missing.
 */
@Entity
@Table(name = "portfolio_projects")
public class PortfolioProject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @Column(nullable = false)
    private String name;

    /** "Brand identity", "Product design", "Frontend" - the templates label projects with it. */
    private String discipline;

    /**
     * Free text rather than a number: owners write "2024", "2023-24", "Ongoing".
     *
     * The column is project_year, not year: H2 - which the test profile builds
     * its schema on - reserves YEAR as an identifier, so Hibernate's generated
     * CREATE TABLE failed there and the whole table was silently absent from
     * every test run. Postgres accepts the bare word, which is why V15 created
     * it and production never noticed. The Java field and the API keep the name
     * "year"; only the column moved.
     */
    @Column(name = "project_year")
    private String year;

    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * Comma-separated, stored as one column on purpose. A join table for what
     * is only ever rendered as a row of labels would add a query and a
     * migration for no gain; the API splits and trims it.
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "live_url", columnDefinition = "TEXT")
    private String liveUrl;

    @Column(name = "repo_url", columnDefinition = "TEXT")
    private String repoUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public BusinessWebsite getWebsite() { return website; }
    public void setWebsite(BusinessWebsite website) { this.website = website; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLiveUrl() { return liveUrl; }
    public void setLiveUrl(String liveUrl) { this.liveUrl = liveUrl; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
