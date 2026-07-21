CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    application_id BIGINT REFERENCES applications(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_chat_messages_shift_id ON chat_messages(shift_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);

CREATE TABLE mock_payments (
    id BIGSERIAL PRIMARY KEY,
    shift_id BIGINT NOT NULL REFERENCES shifts(id),
    worker_id BIGINT REFERENCES users(id),
    manager_id BIGINT REFERENCES users(id),
    amount DOUBLE PRECISION,
    status VARCHAR(50),
    method_label VARCHAR(100),
    created_at TIMESTAMP,
    paid_at TIMESTAMP,
    CONSTRAINT uk_mock_payments_shift UNIQUE (shift_id)
);

CREATE INDEX idx_mock_payments_worker_id ON mock_payments(worker_id);
CREATE INDEX idx_mock_payments_manager_id ON mock_payments(manager_id);
CREATE INDEX idx_mock_payments_status ON mock_payments(status);
