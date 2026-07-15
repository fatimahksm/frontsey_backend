CREATE TABLE support_tickets (
    id                     UUID PRIMARY KEY,
    submitter_account_id   UUID NOT NULL REFERENCES accounts(id),
    category               VARCHAR(40) NOT NULL,
    subject                VARCHAR(255) NOT NULL,
    message                TEXT NOT NULL,
    attachment_url         VARCHAR(500),
    status                 VARCHAR(20) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_support_tickets_submitter ON support_tickets(submitter_account_id);
