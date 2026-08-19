package com.dbwb.platform.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What is actually happening on the platform, from the platform's own records.
 *
 * The Super Admin dashboard was six totals - users, websites, active
 * subscriptions, pending payments, expiring soon, revenue - which says how big
 * the platform is and nothing about how it is doing. None of them move in a way
 * you can read, none of them say which templates people choose, and none
 * separate a first payment from a renewal.
 *
 * Every figure here is counted from rows that already exist: accounts, websites,
 * subscriptions and payments, each with the createdAt BaseEntity gives them.
 * Nothing is modelled, projected or estimated.
 *
 * One thing an admin might expect is deliberately absent: how people sign in.
 * Logins are not recorded anywhere - the audit log covers actions taken on
 * content, not sessions - so there is no honest number to report, and inventing
 * one would be worse than leaving it out.
 */
public record AdminPlatformReportResponse(
        /** The window these series cover, in days. */
        int days,

        /** Which templates owners actually pick, and how many of those reached publish. */
        List<TemplateUsage> templates,

        /** New accounts per day. */
        List<DailyCount> signups,
        /** Websites created per day. */
        List<DailyCount> websitesCreated,
        /** Websites published per day - the moment a site becomes real. */
        List<DailyCount> websitesPublished,
        /** Money taken per day, from successful payments only. */
        List<DailyAmount> revenue,

        /** Every subscription, by the state it is in right now. */
        List<StatusCount> subscriptions,
        /**
         * Which plans the money comes from, grouped by the plan each
         * subscription is on *today*. A payment does not record the plan it was
         * for, only its amount, so a payment made before a plan change counts
         * under the newer plan. Said plainly on the panel rather than presented
         * as exact.
         */
        List<PlanRevenue> revenueByPlan,

        /** Successful payments that were a subscription's first. */
        long firstPayments,
        /** Successful payments on a subscription that had already paid before - renewals. */
        long renewals,
        /** Websites currently on their free trial. */
        long onFreeTrial,
        /** Trials that ended without ever being paid for. */
        long trialsLapsed
) {
    public record TemplateUsage(String layoutVariant, String templateType, long websites, long published) {
    }

    public record DailyCount(LocalDate date, long count) {
    }

    public record DailyAmount(LocalDate date, BigDecimal amount) {
    }

    public record StatusCount(String status, long count) {
    }

    public record PlanRevenue(String planCode, String billingPeriod, long payments, BigDecimal revenue) {
    }
}
