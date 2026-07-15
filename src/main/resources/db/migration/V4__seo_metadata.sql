CREATE TABLE seo_metadata (
    id                UUID PRIMARY KEY,
    website_id        UUID NOT NULL UNIQUE REFERENCES business_websites(id),
    meta_title        VARCHAR(70),
    meta_description  VARCHAR(160),
    og_image_url      VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
