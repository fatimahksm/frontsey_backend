CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Seeds the two fixed MVP plans (7.2) so subscription checkout works out of
-- the box. Prices and limits are placeholders - the BRD marks the exact
-- feature/limit matrix and pricing as TBD-002/TBD-003; update these rows (or
-- edit them via the future Super Admin plan-management screen) once finalized.

INSERT INTO plans (id, code, billing_period, price, max_websites, max_managers_per_website,
                    max_languages, max_gallery_images, image_storage_limit_mb,
                    analytics_enabled, multi_page_enabled, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'BASIC',   'MONTHLY', 9.99,   1, 1, 1, 10, 200,  FALSE, FALSE, TRUE, now(), now()),
    (gen_random_uuid(), 'BASIC',   'YEARLY',  99.99,  1, 1, 1, 10, 200,  FALSE, FALSE, TRUE, now(), now()),
    (gen_random_uuid(), 'PREMIUM', 'MONTHLY', 24.99,  3, 5, 3, 50, 1000, TRUE,  TRUE,  TRUE, now(), now()),
    (gen_random_uuid(), 'PREMIUM', 'YEARLY',  249.99, 3, 5, 3, 50, 1000, TRUE,  TRUE,  TRUE, now(), now());
