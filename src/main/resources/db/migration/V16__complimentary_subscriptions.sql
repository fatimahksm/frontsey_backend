-- A subscription the platform is not charging for.
--
-- Super Admins need to be able to stand a website up for someone - a partner, a
-- pilot, a case the platform simply does not want to bill - without inventing a
-- fake payment or leaving the site permanently one day from being switched off.
-- Such a subscription is ACTIVE with no end date: publishable, never expiring,
-- never asked to pay, and visibly different from a paid one everywhere it is
-- shown.
--
-- Defaults to false, so every existing subscription keeps its current meaning.
ALTER TABLE subscriptions
    ADD COLUMN complimentary BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN subscriptions.complimentary IS
    'True when a Super Admin granted this website free access; such a subscription is never billed and does not expire.';
