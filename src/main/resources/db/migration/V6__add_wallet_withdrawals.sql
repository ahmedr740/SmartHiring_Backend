CREATE TABLE wallet_withdrawals (
    id BIGSERIAL PRIMARY KEY,
    worker_id BIGINT NOT NULL REFERENCES users(id),
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    method_label VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_wallet_withdrawals_worker_id ON wallet_withdrawals(worker_id);
CREATE INDEX idx_wallet_withdrawals_status ON wallet_withdrawals(status);
