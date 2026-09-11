-- What each account has put in the bucket, and how much of it.
--
-- Uploads were recorded nowhere. An account could post a 5MB image a hundred
-- times an hour - the rate limit's ceiling - for as long as it liked, and
-- nothing in the platform could say how much any owner was storing, let alone
-- refuse the next one. The bill for that arrives at Cloudflare.
--
-- One row per stored object, the thumbnail included: it is a separate object
-- costing separate bytes, and counting only originals would under-report
-- storage by roughly the share the small copies take.
--
-- ON DELETE CASCADE because an account that is gone has no quota to enforce.
-- The objects themselves outlive the row; removing those is a separate job
-- and a separate decision, since deleting an owner's photographs is not
-- something to do as a side effect of a foreign key.
CREATE TABLE uploaded_images (
    id           UUID PRIMARY KEY,
    account_id   UUID         NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    storage_key  VARCHAR(255) NOT NULL UNIQUE,
    byte_size    BIGINT       NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

-- The quota check sums one account's rows on every upload, so that sum must
-- not be a scan of every image on the platform.
CREATE INDEX idx_uploaded_images_account ON uploaded_images (account_id);
