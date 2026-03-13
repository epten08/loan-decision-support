-- V11: Create loan_outcomes table for tracking actual loan performance

CREATE TABLE loan_outcomes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
    outcome VARCHAR(20) NOT NULL,
    days_late INTEGER,
    amount_recovered DECIMAL(15, 2),
    recovery_status VARCHAR(50),
    notes TEXT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by VARCHAR(100) DEFAULT 'SYSTEM'
);

-- Index for efficient lookups
CREATE INDEX idx_loan_outcomes_application ON loan_outcomes(loan_application_id);
CREATE INDEX idx_loan_outcomes_outcome ON loan_outcomes(outcome);
CREATE INDEX idx_loan_outcomes_recorded_at ON loan_outcomes(recorded_at);
