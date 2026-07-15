CREATE TABLE analytics_events (
    id               UUID PRIMARY KEY,
    website_id       UUID NOT NULL REFERENCES business_websites(id),
    event_type       VARCHAR(20) NOT NULL,
    item_id          UUID,
    referral_source  VARCHAR(500),
    device_type      VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_analytics_events_website ON analytics_events(website_id);
CREATE INDEX idx_analytics_events_website_type_created ON analytics_events(website_id, event_type, created_at);
