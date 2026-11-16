CREATE TABLE public_auth_rate_limit_buckets (
    bucket_key VARCHAR(64) NOT NULL,
    window_start_epoch BIGINT NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count > 0),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_public_auth_rate_limit_buckets PRIMARY KEY (bucket_key, window_start_epoch)
);

CREATE INDEX idx_public_auth_rate_limit_buckets_expires_at
    ON public_auth_rate_limit_buckets (expires_at);
