CREATE TABLE IF NOT EXISTS liked_jobs (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES users(id),
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    created_at TIMESTAMP,
    CONSTRAINT uk_liked_jobs_worker_shift UNIQUE (worker_id, shift_id)
);
