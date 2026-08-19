package com.dbwb.platform.plan.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.LayoutVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * What one template costs, monthly and yearly.
 *
 * Price and entitlement are deliberately separate questions. This row answers
 * "what does an owner pay for this template"; the plan it names answers "what
 * does that website get". Keeping them apart is what lets an owner choose a
 * template and a billing period without ever having to reason about tiers.
 *
 * Exactly one row per LayoutVariant, enforced by a unique constraint: a template
 * with no price is a website nobody can pay for.
 */
@Entity
@Table(name = "template_prices")
public class TemplatePrice extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_variant", nullable = false, unique = true, length = 40)
    private LayoutVariant layoutVariant;

    @Column(name = "monthly_price", nullable = false)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", nullable = false)
    private BigDecimal yearlyPrice;

    /** Which plan's limits a website on this template gets. */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, length = 20)
    private PlanCode planCode;

    @Column(nullable = false)
    private boolean active = true;

    /** The figure for one billing period - the single place that choice is made. */
    public BigDecimal priceFor(BillingPeriod billingPeriod) {
        return billingPeriod == BillingPeriod.YEARLY ? yearlyPrice : monthlyPrice;
    }

    public LayoutVariant getLayoutVariant() {
        return layoutVariant;
    }

    public void setLayoutVariant(LayoutVariant layoutVariant) {
        this.layoutVariant = layoutVariant;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public BigDecimal getYearlyPrice() {
        return yearlyPrice;
    }

    public void setYearlyPrice(BigDecimal yearlyPrice) {
        this.yearlyPrice = yearlyPrice;
    }

    public PlanCode getPlanCode() {
        return planCode;
    }

    public void setPlanCode(PlanCode planCode) {
        this.planCode = planCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
