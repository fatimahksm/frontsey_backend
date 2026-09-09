package com.dbwb.platform.events.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The facts of one occasion: when it is, where, and what to know before coming.
 *
 * One row per website, like BusinessProfile - an EVENTS website is one event,
 * not a listing of many. Where and how to reach the hosts stay on
 * BusinessProfile (address, googleMapsUrl, phone, whatsappNumber) rather than
 * being duplicated here; this holds only what a profile has nowhere to put.
 *
 * Every field is nullable. An invitation that says only "Saturday, at ours" is
 * a real invitation, and the template hides whatever is missing.
 */
@Entity
@Table(name = "event_details")
public class EventDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false, unique = true)
    private BusinessWebsite website;

    /**
     * Free text rather than a date type, deliberately. Owners write "14 June
     * 2026", "Saturday the 14th", "next spring" - and a date picker that
     * refuses the third is a worse invitation, not a tidier one. Nothing on the
     * platform sorts or filters by it.
     */
    @Column(name = "event_date")
    private String eventDate;

    /** Also free text: "6:00 PM", "after sunset", "doors at 7". */
    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "venue_name")
    private String venueName;

    /** "Black tie", "come as you are". */
    @Column(name = "dress_code")
    private String dressCode;

    /** What guests should reply by - again free text, because "by the end of May" is a normal thing to write. */
    @Column(name = "rsvp_by")
    private String rsvpBy;

    /** Anything else the hosts want said: parking, children, gifts. */
    @Column(columnDefinition = "TEXT")
    private String note;

    public BusinessWebsite getWebsite() { return website; }
    public void setWebsite(BusinessWebsite website) { this.website = website; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getDressCode() { return dressCode; }
    public void setDressCode(String dressCode) { this.dressCode = dressCode; }

    public String getRsvpBy() { return rsvpBy; }
    public void setRsvpBy(String rsvpBy) { this.rsvpBy = rsvpBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
