-- Initial schema for the Dynamic Business Website Builder MVP (BRD v1.0).
-- Table/column shapes mirror the JPA entities exactly, since Hibernate is
-- configured with ddl-auto=validate (Flyway owns schema, Hibernate only checks it).

CREATE TABLE accounts (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255),
    role              VARCHAR(30)  NOT NULL,
    status            VARCHAR(40)  NOT NULL,
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    disabled_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE TABLE account_tokens (
    id          UUID PRIMARY KEY,
    account_id  UUID NOT NULL REFERENCES accounts(id),
    token       VARCHAR(255) NOT NULL UNIQUE,
    type        VARCHAR(30)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_account_tokens_account ON account_tokens(account_id);

CREATE TABLE themes (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    theme_config  TEXT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE plans (
    id                        UUID PRIMARY KEY,
    code                      VARCHAR(20) NOT NULL,
    billing_period            VARCHAR(20) NOT NULL,
    price                     NUMERIC(10,2) NOT NULL,
    max_websites              INTEGER NOT NULL DEFAULT 0,
    max_managers_per_website  INTEGER NOT NULL DEFAULT 0,
    max_languages             INTEGER NOT NULL DEFAULT 1,
    max_gallery_images        INTEGER NOT NULL DEFAULT 0,
    image_storage_limit_mb    BIGINT NOT NULL DEFAULT 0,
    analytics_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    multi_page_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    active                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ NOT NULL,
    updated_at                TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_plan_code_period UNIQUE (code, billing_period)
);

CREATE TABLE business_websites (
    id                          UUID PRIMARY KEY,
    owner_account_id            UUID NOT NULL REFERENCES accounts(id),
    business_name               VARCHAR(255) NOT NULL,
    slug                        VARCHAR(255) NOT NULL UNIQUE,
    theme_id                    UUID REFERENCES themes(id),
    page_mode                   VARCHAR(20) NOT NULL,
    ordering_mode                VARCHAR(30) NOT NULL,
    status                      VARCHAR(30) NOT NULL,
    primary_language             VARCHAR(10) NOT NULL DEFAULT 'en',
    currency                    VARCHAR(10) NOT NULL DEFAULT 'USD',
    draft_content               TEXT,
    published_content            TEXT,
    previous_published_content    TEXT,
    published_at                 TIMESTAMPTZ,
    trashed_at                  TIMESTAMPTZ,
    suspension_reason            VARCHAR(500),
    suspension_reactivate_at       TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_websites_owner ON business_websites(owner_account_id);

CREATE TABLE business_profiles (
    id                              UUID PRIMARY KEY,
    website_id                      UUID NOT NULL UNIQUE REFERENCES business_websites(id),
    description                    TEXT,
    logo_url                       VARCHAR(500),
    cover_image_url                 VARCHAR(500),
    phone                           VARCHAR(50),
    whatsapp_number                  VARCHAR(50),
    email                           VARCHAR(255),
    address                         VARCHAR(500),
    google_maps_url                  VARCHAR(500),
    instagram_url                    VARCHAR(500),
    tiktok_url                      VARCHAR(500),
    show_privacy_policy               BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_policy_content            TEXT,
    show_terms_and_conditions           BOOLEAN NOT NULL DEFAULT FALSE,
    terms_and_conditions_content        TEXT,
    show_delivery_policy              BOOLEAN NOT NULL DEFAULT FALSE,
    delivery_policy_content            TEXT,
    show_refund_policy                BOOLEAN NOT NULL DEFAULT FALSE,
    refund_policy_content              TEXT,
    created_at                      TIMESTAMPTZ NOT NULL,
    updated_at                      TIMESTAMPTZ NOT NULL
);

CREATE TABLE opening_hours (
    id          UUID PRIMARY KEY,
    website_id  UUID NOT NULL REFERENCES business_websites(id),
    day_of_week VARCHAR(15) NOT NULL,
    open        BOOLEAN NOT NULL DEFAULT FALSE,
    opens_at    TIME,
    closes_at   TIME,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_opening_hours_website ON opening_hours(website_id);

CREATE TABLE manager_access (
    id                  UUID PRIMARY KEY,
    website_id          UUID NOT NULL REFERENCES business_websites(id),
    manager_account_id  UUID REFERENCES accounts(id),
    invited_email       VARCHAR(255) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_manager_access_website ON manager_access(website_id);
CREATE INDEX idx_manager_access_account ON manager_access(manager_account_id);

CREATE TABLE manager_access_permissions (
    manager_access_id  UUID NOT NULL REFERENCES manager_access(id),
    permission         VARCHAR(40) NOT NULL
);

CREATE TABLE menu_categories (
    id          UUID PRIMARY KEY,
    website_id  UUID NOT NULL REFERENCES business_websites(id),
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_categories_website ON menu_categories(website_id);

CREATE TABLE menu_items (
    id                   UUID PRIMARY KEY,
    website_id           UUID NOT NULL REFERENCES business_websites(id),
    category_id          UUID NOT NULL REFERENCES menu_categories(id),
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    ingredients          TEXT,
    price                NUMERIC(10,2) NOT NULL,
    discount_price       NUMERIC(10,2),
    image_url            VARCHAR(500),
    availability         VARCHAR(20) NOT NULL,
    unavailable_until    TIMESTAMPTZ,
    max_order_quantity   INTEGER,
    fixed_box_item       BOOLEAN NOT NULL DEFAULT FALSE,
    trashed_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_items_website ON menu_items(website_id);
CREATE INDEX idx_items_category ON menu_items(category_id);

CREATE TABLE menu_item_sizes (
    id           UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    label        VARCHAR(100) NOT NULL,
    price        NUMERIC(10,2) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE menu_item_addon_groups (
    id             UUID PRIMARY KEY,
    menu_item_id   UUID NOT NULL REFERENCES menu_items(id),
    name           VARCHAR(100),
    max_selections INTEGER,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE menu_item_addons (
    id             UUID PRIMARY KEY,
    addon_group_id UUID NOT NULL REFERENCES menu_item_addon_groups(id),
    name           VARCHAR(100) NOT NULL,
    extra_price    NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE menu_item_box_variants (
    id            UUID PRIMARY KEY,
    menu_item_id  UUID NOT NULL REFERENCES menu_items(id),
    label         VARCHAR(100) NOT NULL,
    unit_count    INTEGER NOT NULL,
    price         NUMERIC(10,2) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE delivery_areas (
    id                       UUID PRIMARY KEY,
    website_id               UUID NOT NULL REFERENCES business_websites(id),
    name                     VARCHAR(255) NOT NULL,
    delivery_fee              NUMERIC(10,2) NOT NULL,
    minimum_order_amount        NUMERIC(10,2) NOT NULL,
    free_delivery_threshold     NUMERIC(10,2),
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_delivery_areas_website ON delivery_areas(website_id);

CREATE TABLE subscriptions (
    id             UUID PRIMARY KEY,
    website_id     UUID NOT NULL UNIQUE REFERENCES business_websites(id),
    plan_id        UUID NOT NULL REFERENCES plans(id),
    status         VARCHAR(20) NOT NULL,
    start_date     TIMESTAMPTZ,
    end_date       TIMESTAMPTZ,
    grace_ends_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE mock_payments (
    id               UUID PRIMARY KEY,
    subscription_id  UUID NOT NULL REFERENCES subscriptions(id),
    amount           NUMERIC(10,2) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    reference        VARCHAR(100) NOT NULL UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_mock_payments_subscription ON mock_payments(subscription_id);

CREATE TABLE notifications (
    id                    UUID PRIMARY KEY,
    recipient_account_id  UUID NOT NULL,
    event                 VARCHAR(40) NOT NULL,
    message               VARCHAR(1000) NOT NULL,
    read                  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_account_id);

CREATE TABLE audit_logs (
    id                UUID PRIMARY KEY,
    actor_account_id  UUID NOT NULL,
    action            VARCHAR(100) NOT NULL,
    target_id         VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_actor ON audit_logs(actor_account_id);

CREATE TABLE gallery_images (
    id          UUID PRIMARY KEY,
    website_id  UUID NOT NULL REFERENCES business_websites(id),
    image_url   VARCHAR(500) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    cover       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_gallery_website ON gallery_images(website_id);
