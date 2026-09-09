-- The shop gets its own kind of website.
--
-- It was a MENU_ORDERING site carrying "menuBusinessKind": "SHOP" in its
-- draft content, which renamed the labels and swapped the sample photos but
-- still handed a shop a restaurant's page. Two layouts of its own now: a
-- photo-led shop front, and a searchable catalogue for shops with more stock
-- than anyone will scroll.
--
-- Deliberately no UPDATE of existing websites. A site already published on
-- MENU_GRID with the shop flag is live and looks the way its owner approved;
-- moving it to a template they have never seen would change a published page
-- underneath them. They keep rendering exactly as before, and can switch from
-- the Layout tab whenever they want to.
INSERT INTO template_prices (id, layout_variant, monthly_price, yearly_price, plan_code, active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'STORE_SHOWCASE', 9.99, 99.99, 'BASIC', TRUE, now(), now()),
    (gen_random_uuid(), 'STORE_CATALOG', 9.99, 99.99, 'BASIC', TRUE, now(), now())
ON CONFLICT (layout_variant) DO NOTHING;
