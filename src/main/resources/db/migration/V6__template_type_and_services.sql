ALTER TABLE business_websites
    ADD COLUMN template_type VARCHAR(20) NOT NULL DEFAULT 'MENU_ORDERING';

CREATE TABLE services (
    id                  UUID PRIMARY KEY,
    website_id          UUID NOT NULL REFERENCES business_websites(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    price               NUMERIC(10, 2),
    image_url           VARCHAR(500),
    sort_order          INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_services_website ON services(website_id);
