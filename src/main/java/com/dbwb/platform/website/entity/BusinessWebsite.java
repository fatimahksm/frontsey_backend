package com.dbwb.platform.website.entity;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.theme.entity.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * BR-SITE-007/008: one row per tenant website, reached via a unique slug
 * (platform.com/{slug}), representing exactly one business branch.
 *
 * Draft vs. published separation (BR-THEME-004): "content" holds the
 * structured section/branding configuration currently being edited.
 * "publishedContent" is what the public site actually renders.
 * "previousPublishedContent" retains exactly one prior version for the
 * BR-THEME-007 rollback rule - no deeper history in the MVP.
 */
@Entity
@Table(name = "business_websites")
public class BusinessWebsite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_account_id", nullable = false)
    private Account owner;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageMode pageMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderingMode orderingMode = OrderingMode.DISPLAY_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebsiteStatus status = WebsiteStatus.DRAFT;

    /** ISO 639-1 code; multi-language support gated by plan (BR-LANG-001). */
    @Column(nullable = false)
    private String primaryLanguage = "en";

    /** BR-PROF-005: one currency per website (ISO 4217 code). */
    @Column(nullable = false)
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String draftContent;

    @Column(columnDefinition = "TEXT")
    private String publishedContent;

    @Column(columnDefinition = "TEXT")
    private String previousPublishedContent;

    private Instant publishedAt;

    private Instant trashedAt;

    /** Internal reason for suspension - never exposed on the public page (BR-ADM-005). */
    private String suspensionReason;
    private Instant suspensionReactivateAt;

    // --- getters / setters ---

    public Account getOwner() {
        return owner;
    }

    public void setOwner(Account owner) {
        this.owner = owner;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public PageMode getPageMode() {
        return pageMode;
    }

    public void setPageMode(PageMode pageMode) {
        this.pageMode = pageMode;
    }

    public OrderingMode getOrderingMode() {
        return orderingMode;
    }

    public void setOrderingMode(OrderingMode orderingMode) {
        this.orderingMode = orderingMode;
    }

    public WebsiteStatus getStatus() {
        return status;
    }

    public void setStatus(WebsiteStatus status) {
        this.status = status;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDraftContent() {
        return draftContent;
    }

    public void setDraftContent(String draftContent) {
        this.draftContent = draftContent;
    }

    public String getPublishedContent() {
        return publishedContent;
    }

    public void setPublishedContent(String publishedContent) {
        this.publishedContent = publishedContent;
    }

    public String getPreviousPublishedContent() {
        return previousPublishedContent;
    }

    public void setPreviousPublishedContent(String previousPublishedContent) {
        this.previousPublishedContent = previousPublishedContent;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getTrashedAt() {
        return trashedAt;
    }

    public void setTrashedAt(Instant trashedAt) {
        this.trashedAt = trashedAt;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }

    public Instant getSuspensionReactivateAt() {
        return suspensionReactivateAt;
    }

    public void setSuspensionReactivateAt(Instant suspensionReactivateAt) {
        this.suspensionReactivateAt = suspensionReactivateAt;
    }
}
