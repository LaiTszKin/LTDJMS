-- Base schema for BIGSERIAL compatibility test: id column stored as BIGINT in the database.
CREATE TABLE IF NOT EXISTS bigserial_compat_test (
    id BIGINT PRIMARY KEY
);
