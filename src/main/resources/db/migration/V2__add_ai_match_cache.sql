CREATE TABLE IF NOT EXISTS ai_match_cache (
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
