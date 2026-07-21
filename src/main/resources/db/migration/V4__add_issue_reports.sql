CREATE TABLE issue_reports (
    id BIGSERIAL PRIMARY KEY,
    reported_by_id BIGINT REFERENCES users(id),
    application_id BIGINT REFERENCES applications(id),
    shift_id BIGINT REFERENCES shifts(id),
    category VARCHAR(100),
    description VARCHAR(1000),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_issue_reports_reported_by_id ON issue_reports(reported_by_id);
CREATE INDEX idx_issue_reports_shift_id ON issue_reports(shift_id);
CREATE INDEX idx_issue_reports_status ON issue_reports(status);
