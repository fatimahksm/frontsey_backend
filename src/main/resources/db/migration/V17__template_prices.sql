-- A price per template, with a monthly and a yearly figure.
--
-- Until now a website's price came from its plan (BASIC/PREMIUM), so every menu
-- template cost the same as every other. Templates are not equally expensive to
-- offer, and the platform wants to price them apart - a plain price list and a
-- full ordering grid should not carry the same monthly fee.
--
-- The plan is still what a website *gets* (limits, analytics, multi-page); this
-- table is what the owner *pays*. Keeping them apart means an owner never has to
-- reason about tiers: they pick a template, then monthly or yearly.
CREATE TABLE template_prices (
    id              UUID PRIMARY KEY,
    layout_variant  VARCHAR(40) NOT NULL UNIQUE,
    monthly_price   NUMERIC(10,2) NOT NULL,
    yearly_price    NUMERIC(10,2) NOT NULL,
    -- Which plan's limits a website on this template gets. Pricing and
    -- entitlement are separate questions, and this is where they are joined.
    plan_code       VARCHAR(20) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

-- Seeded at the current BASIC prices, so nothing changes for anybody until a
-- Super Admin edits a row. Every template exists here from the start: a missing
-- row would mean a website that cannot be paid for.
INSERT INTO template_prices (id, layout_variant, monthly_price, yearly_price, plan_code, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'MENU_CLASSIC',      9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'MENU_GRID',         9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'MENU_ELEGANT',      9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'MENU_BISTRO',       9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'PORTFOLIO_HERO',    9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'PORTFOLIO_MINIMAL', 9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'PORTFOLIO_BOLD',    9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'PORTFOLIO_PROFILE', 9.99, 99.99, 'BASIC', TRUE, now(), now());
