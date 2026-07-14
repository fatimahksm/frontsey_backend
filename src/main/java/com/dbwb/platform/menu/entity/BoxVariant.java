package com.dbwb.platform.menu.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * BR-OPT-004/005: e.g. "Box of 4", "Box of 6", "Box of 12" - each with its
 * own price and a fixed content quantity the customer cannot alter
 * (BR-RULE-010: quantity changes only by whole boxes, enforced in the cart/frontend).
 */
@Entity
@Table(name = "menu_item_box_variants")
public class BoxVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private String label; // e.g. "Box of 6"

    @Column(nullable = false)
    private int unitCount; // e.g. 6

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public void setUnitCount(int unitCount) {
        this.unitCount = unitCount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
