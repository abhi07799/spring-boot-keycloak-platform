-- Remove the unplanned rate-limiting feature while preserving Flyway migration history.
DROP TABLE IF EXISTS public_auth_rate_limit_buckets;
