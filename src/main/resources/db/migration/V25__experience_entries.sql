-- The work history shown on the PORTFOLIO templates.
--
-- Two of the four portfolio layouts already render an experience timeline,
-- and they read it from `about.experience` in a section's free-form JSON -
-- which no editor in the console can write. So the timeline appears on the
-- seeded sample sites and can never appear on a real owner's, because there
-- has never been anywhere for them to type it.
--
-- Same shape and same treatment as portfolio_projects: nullable everywhere
-- the templates already hide what is missing, an explicit sort order the
-- owner controls, and ON DELETE CASCADE from the website.
--
-- The year is free text, not a number - people write "2024", "2023-24",
-- "Present". Named entry_year rather than year for the reason V18 renamed
-- the projects column: H2, which the test profile builds its schema on,
-- reserves YEAR as an identifier, so a column called year is silently absent
-- from every test run while Postgres accepts it happily.
CREATE TABLE IF NOT EXISTS experience_entries (
    id          UUID PRIMARY KEY,
    website_id  UUID NOT NULL REFERENCES business_websites(id) ON DELETE CASCADE,
    role        VARCHAR(255) NOT NULL,
    company     VARCHAR(255),
    entry_year  VARCHAR(64),
    detail      TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_experience_entries_website ON experience_entries (website_id, sort_order);
