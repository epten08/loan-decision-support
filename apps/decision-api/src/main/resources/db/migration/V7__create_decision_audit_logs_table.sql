-- V7: Create decision_audit_logs table
CREATE TABLE decision_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_id UUID NOT NULL REFERENCES decisions(id),
    event_type VARCHAR(30) NOT NULL,
    event_description TEXT,
    previous_state VARCHAR(50),
    new_state VARCHAR(50),
    rule_evaluation_summary TEXT,
    risk_assessment_summary TEXT,
    performed_by VARCHAR(100) DEFAULT 'SYSTEM',
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_decision ON decision_audit_logs(decision_id);
CREATE INDEX idx_audit_logs_event_type ON decision_audit_logs(event_type);
CREATE INDEX idx_audit_logs_timestamp ON decision_audit_logs(event_timestamp);
