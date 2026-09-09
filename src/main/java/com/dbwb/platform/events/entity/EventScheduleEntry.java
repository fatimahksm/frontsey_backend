package com.dbwb.platform.events.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One line in the running order of the day - "7:00 PM - Ceremony". Ordered by the host, not by the clock. */
@Entity
@Table(name = "event_schedule_entries")
public class EventScheduleEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    /** Free text, same reasoning as EventDetails: "7:00 PM", "at sunset", "after dinner". */
    @Column(name = "entry_time")
    private String time;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public BusinessWebsite getWebsite() { return website; }
    public void setWebsite(BusinessWebsite website) { this.website = website; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
