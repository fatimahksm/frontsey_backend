-- Projects shown on the PORTFOLIO templates.
--
-- Until now a portfolio's projects were only gallery image URLs, so the
-- rebuilt templates had nowhere to read a project's name, discipline or links
-- from and carried them in a section's free-form JSON instead - which no
-- editor could write. This gives them a table of their own.
--
-- Every column but the name is nullable: a project with only a picture and a
-- title is a legitimate portfolio entry, and the templates already hide what
-- is absent.
CREATE TABLE IF NOT EXISTS portfolio_projects (
    id             UUID PRIMARY KEY,
    website_id     UUID NOT NULL REFERENCES business_websites(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    discipline     VARCHAR(255),
    year           VARCHAR(32),
    summary        TEXT,
    tags           TEXT,
    image_url      TEXT,
    live_url       TEXT,
    repo_url       TEXT,
    sort_order     INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_portfolio_projects_website ON portfolio_projects(website_id, sort_order);
