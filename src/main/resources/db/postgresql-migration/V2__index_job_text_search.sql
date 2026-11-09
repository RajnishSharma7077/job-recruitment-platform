CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_jobs_title_trgm ON jobs USING GIN (lower(title) gin_trgm_ops);
CREATE INDEX idx_jobs_location_trgm ON jobs USING GIN (lower(location) gin_trgm_ops);
CREATE INDEX idx_jobs_description_trgm ON jobs USING GIN (lower(description) gin_trgm_ops);
