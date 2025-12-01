-- Canonical schema for BIGSERIAL compatibility test: id column defined as BIGSERIAL.
CREATE TABLE IF NOT EXISTS bigserial_compat_test (
    id BIGSERIAL PRIMARY KEY
);
