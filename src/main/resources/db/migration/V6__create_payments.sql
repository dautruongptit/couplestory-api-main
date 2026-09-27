CREATE TABLE payments (
    id               UUID PRIMARY KEY,
    story_id         UUID NOT NULL REFERENCES stories (id) ON DELETE CASCADE,
    amount           BIGINT NOT NULL,
    currency         VARCHAR(10) NOT NULL DEFAULT 'VND',
    provider         VARCHAR(30) NOT NULL DEFAULT 'vnpay',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref  VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_payments_transaction_ref ON payments (transaction_ref) WHERE transaction_ref IS NOT NULL;
CREATE INDEX ix_payments_story_id ON payments (story_id);
