-- Renames the four portfolio LayoutVariant values to match what they render.
--
-- The names were kept when the templates were rebuilt around four audiences,
-- so they had come to say the opposite of the truth: PORTFOLIO_HERO renders
-- the Professional / CV template, PORTFOLIO_MINIMAL the Creative / Visual one,
-- PORTFOLIO_BOLD Brand / Product, PORTFOLIO_PROFILE Freelancer / Services. The
-- new names also match the component that draws each one
-- (PublicPortfolioSiteProfessional, ...Visual, ...Brand, ...Services).
--
-- Both tables that persist the enum are mapped. Missing either would leave
-- rows that no longer deserialise: business_websites.layout_variant fails on
-- read for that website, and template_prices.layout_variant is NOT NULL UNIQUE
-- and seeded by V17, so a stale row there breaks the pricing lookup and blocks
-- re-seeding the correct one.
--
-- MENU_* values are untouched; their names were always accurate.

UPDATE business_websites SET layout_variant = 'PORTFOLIO_PROFESSIONAL' WHERE layout_variant = 'PORTFOLIO_HERO';
UPDATE business_websites SET layout_variant = 'PORTFOLIO_VISUAL'       WHERE layout_variant = 'PORTFOLIO_MINIMAL';
UPDATE business_websites SET layout_variant = 'PORTFOLIO_BRAND'        WHERE layout_variant = 'PORTFOLIO_BOLD';
UPDATE business_websites SET layout_variant = 'PORTFOLIO_SERVICES'     WHERE layout_variant = 'PORTFOLIO_PROFILE';

UPDATE template_prices SET layout_variant = 'PORTFOLIO_PROFESSIONAL' WHERE layout_variant = 'PORTFOLIO_HERO';
UPDATE template_prices SET layout_variant = 'PORTFOLIO_VISUAL'       WHERE layout_variant = 'PORTFOLIO_MINIMAL';
UPDATE template_prices SET layout_variant = 'PORTFOLIO_BRAND'        WHERE layout_variant = 'PORTFOLIO_BOLD';
UPDATE template_prices SET layout_variant = 'PORTFOLIO_SERVICES'     WHERE layout_variant = 'PORTFOLIO_PROFILE';
