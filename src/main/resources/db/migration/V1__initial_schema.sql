CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50),
    status VARCHAR(50),
    skills VARCHAR(1000),
    rating DOUBLE PRECISION,
    rating_count INTEGER,
    completed_shifts_count INTEGER,
    restaurant_name VARCHAR(255),
    phone VARCHAR(100),
    location VARCHAR(255),
    availability VARCHAR(1000),
    created_at TIMESTAMP
);

CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    date VARCHAR(50),
    start_time VARCHAR(50),
    end_time VARCHAR(50),
    pay DOUBLE PRECISION,
    role_needed VARCHAR(255),
    location VARCHAR(255),
    status VARCHAR(50),
    paid BOOLEAN,
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    paid_at TIMESTAMP,
    manager_id BIGINT REFERENCES users(id),
    assigned_worker_id BIGINT REFERENCES users(id)
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT REFERENCES users(id),
    shift_id BIGINT REFERENCES shifts(id),
    status VARCHAR(50),
    worker_rating INTEGER,
    worker_review VARCHAR(1000),
    worker_rated_at TIMESTAMP,
    manager_rating INTEGER,
    manager_review VARCHAR(1000),
    manager_rated_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE TABLE ai_match_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(255) UNIQUE NOT NULL,
    target_id BIGINT NOT NULL,
    ai_score INTEGER,
    fallback_score INTEGER NOT NULL,
    label VARCHAR(100),
    explanation VARCHAR(1000),
    strengths VARCHAR(1000),
    risks VARCHAR(1000),
    recommended_action VARCHAR(255),
    source VARCHAR(50) NOT NULL,
    generated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_shifts_manager_id ON shifts(manager_id);
CREATE INDEX idx_applications_shift_id ON applications(shift_id);
CREATE INDEX idx_applications_worker_id ON applications(worker_id);
