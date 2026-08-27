-- The Events / Memories template: one occasion, and the pictures from it.
--
-- Its own TemplateType rather than a portfolio layout, because the questions
-- are different - an event has one date, one venue and a running order, and a
-- services showcase has nowhere to put any of them.
--
-- Where and how to reach the hosts stay on business_profiles (address,
-- google_maps_url, phone, whatsapp_number); this holds only what a profile has
-- nowhere to put. The photographs are gallery_images, which already exist.

CREATE TABLE IF NOT EXISTS event_details (
    id           UUID PRIMARY KEY,
    -- One event per website, so a unique constraint rather than a plain FK:
    -- an EVENTS website *is* the occasion, it does not list several.
    website_id   UUID NOT NULL UNIQUE REFERENCES business_websites(id) ON DELETE CASCADE,
    -- Free text, not dates and times. Hosts write "14 June 2026", "Saturday the
    -- 14th", "after sunset" - a picker that refuses the third makes for a worse
    -- invitation, not a tidier one, and nothing here sorts or filters by it.
    event_date   VARCHAR(120),
    start_time   VARCHAR(60),
    end_time     VARCHAR(60),
    venue_name   VARCHAR(255),
    dress_code   VARCHAR(120),
    rsvp_by      VARCHAR(120),
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS event_schedule_entries (
    id           UUID PRIMARY KEY,
    website_id   UUID NOT NULL REFERENCES business_websites(id) ON DELETE CASCADE,
    entry_time   VARCHAR(60),
    title        VARCHAR(255) NOT NULL,
    detail       TEXT,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_schedule_website ON event_schedule_entries(website_id, sort_order);

-- Priced, but switched off. A template with no price row counts as not offered
-- (TemplateAvailability), so it needs one to be switchable at all - and active
-- FALSE is what keeps it out of every picker until an admin turns it on.
INSERT INTO template_prices (id, layout_variant, monthly_price, yearly_price, plan_code, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'EVENTS_CELEBRATION', 9.99, 99.99, 'BASIC', FALSE, now(), now())
ON CONFLICT (layout_variant) DO NOTHING;
