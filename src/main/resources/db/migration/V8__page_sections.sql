CREATE TABLE page_sections (
    id                  UUID PRIMARY KEY,
    website_id          UUID NOT NULL REFERENCES business_websites(id),
    type                VARCHAR(30) NOT NULL,
    data                TEXT NOT NULL,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_page_sections_website ON page_sections(website_id);
