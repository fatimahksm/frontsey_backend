-- Phase 4 (BR-MGR-001): a website must never have two active (pending or
-- accepted) manager-access rows for the same invited email. Enforced at the
-- database level in addition to the application check in ManagerService,
-- so this can never regress via a future code path that forgets the check.
-- A partial index (not a plain UNIQUE constraint) so a revoked/rejected/
-- expired row never blocks re-inviting the same email.
CREATE UNIQUE INDEX uq_manager_access_active_invite
    ON manager_access (website_id, lower(invited_email))
    WHERE status IN ('PENDING', 'ACCEPTED');
