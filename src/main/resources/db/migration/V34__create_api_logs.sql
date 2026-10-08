-- Technical request log: one row per API call (route template, status, duration, who, from where).
-- Never stores query strings, headers or bodies. Rows older than 30 days are purged by ApiLogRetentionJob.
CREATE TABLE api_logs (
    id          BIGSERIAL PRIMARY KEY,
    request_id  VARCHAR(36)  NOT NULL,
    user_id     UUID,                         -- no foreign key: logs outlive the account
    method      VARCHAR(10)  NOT NULL,
    route       VARCHAR(200) NOT NULL,        -- route template such as /api/stories/{id}
    status      SMALLINT     NOT NULL,
    duration_ms INTEGER      NOT NULL,
    ip          VARCHAR(45),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_api_logs_created_at ON api_logs (created_at);
CREATE INDEX ix_api_logs_user_created ON api_logs (user_id, created_at DESC) WHERE user_id IS NOT NULL;
CREATE INDEX ix_api_logs_errors_created ON api_logs (created_at DESC) WHERE status >= 500;
