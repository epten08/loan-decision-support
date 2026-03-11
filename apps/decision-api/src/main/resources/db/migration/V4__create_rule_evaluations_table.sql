-- V4: Create rule_evaluations table
CREATE TABLE rule_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL REFERENCES loan_applications(id),
    rule_code VARCHAR(50) NOT NULL,
    passed BOOLEAN NOT NULL,
    severity VARCHAR(10) NOT NULL,
    reason VARCHAR(255),
    evaluated_condition TEXT,
    actual_value TEXT,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rule_evaluations_application ON rule_evaluations(loan_application_id);
CREATE INDEX idx_rule_evaluations_rule_code ON rule_evaluations(rule_code);
CREATE INDEX idx_rule_evaluations_passed ON rule_evaluations(passed);
