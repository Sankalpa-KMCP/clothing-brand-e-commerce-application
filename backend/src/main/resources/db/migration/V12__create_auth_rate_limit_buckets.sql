CREATE TABLE auth_rate_limit_buckets (
    key_hash VARCHAR(64) PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_rate_limit_buckets_action ON auth_rate_limit_buckets(action);
CREATE INDEX idx_auth_rate_limit_buckets_window_end ON auth_rate_limit_buckets(window_end);
