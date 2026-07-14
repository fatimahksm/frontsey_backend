package com.dbwb.platform.menu.entity;

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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * BR-MENU-003/004/006/007: core menu item fields. BR-OPT-004..007: a
 * "fixed box" item (e.g. Donut Box) is modeled as one item whose price/sizing
 * is expressed through BoxVariant rows instead of the SizeVariant table -
 * an item is either a simple item (with optional SizeVariants) or a fixed-box
 * item (with BoxVariants), never both, enforced in the service layer.
 */
@Entity
@Table(name = "menu_items")
public class MenuItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String ingredients; // BR-MENU-003 (free text list; allergen tagging is out of MVP scope)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** BR-RULE-007: must be less than price and non-negative when present. */
    private BigDecimal discountPrice;

    private String imageUrl; // BR-MENU-004: optional, themes must handle missing images

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemAvailability availability = ItemAvailability.AVAILABLE;

    /** BR-MENU-006: temporary unavailability window; item auto-reverts to AVAILABLE after this. */
    private Instant unavailableUntil;

    /** BR-MENU-007: optional per-item max quantity enforced by the cart. */
    private Integer maxOrderQuantity;

    /** BR-OPT-004: true when this item is modeled as a fixed box with BoxVariant rows. */
    private boolean fixedBoxItem = false;

    private Instant trashedAt; // BR-MENU-011: 7-day trash window

    // --- getters / setters ---

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ItemAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(ItemAvailability availability) {
        this.availability = availability;
    }

    public Instant getUnavailableUntil() {
        return unavailableUntil;
    }

    public void setUnavailableUntil(Instant unavailableUntil) {
        this.unavailableUntil = unavailableUntil;
    }

    public Integer getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(Integer maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }

    public boolean isFixedBoxItem() {
        return fixedBoxItem;
    }

    public void setFixedBoxItem(boolean fixedBoxItem) {
        this.fixedBoxItem = fixedBoxItem;
    }

    public Instant getTrashedAt() {
        return trashedAt;
    }

    public void setTrashedAt(Instant trashedAt) {
        this.trashedAt = trashedAt;
    }
}
