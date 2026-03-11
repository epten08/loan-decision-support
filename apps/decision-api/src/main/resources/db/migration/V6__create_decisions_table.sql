-- V6: Create decisions table
CREATE TABLE decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
    outcome VARCHAR(20) NOT NULL,
    reason_codes TEXT,
    risk_band VARCHAR(5),
    hard_rule_failures INTEGER,
    soft_rule_failures INTEGER,
    decision_summary TEXT,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by VARCHAR(100) DEFAULT 'SYSTEM'
);

CREATE INDEX idx_decisions_application ON decisions(loan_application_id);
CREATE INDEX idx_decisions_outcome ON decisions(outcome);
CREATE INDEX idx_decisions_decided_at ON decisions(decided_at);
