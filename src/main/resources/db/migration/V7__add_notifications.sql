CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    action_url VARCHAR(255),
    read_at TIMESTAMP,
    email_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    email_status VARCHAR(30) NOT NULL DEFAULT 'NOT_APPLICABLE',
    email_attempts INTEGER NOT NULL DEFAULT 0,
    last_email_attempt_at TIMESTAMP,
    dedupe_key VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications(recipient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_email_dispatch
    ON notifications(email_eligible, email_status, email_attempts, created_at);
