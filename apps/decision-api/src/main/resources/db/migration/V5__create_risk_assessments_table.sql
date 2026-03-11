-- V5: Create risk_assessments table
CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
    probability_of_default DECIMAL(10, 6) NOT NULL,
    risk_band VARCHAR(5) NOT NULL,
    confidence DECIMAL(5, 4) NOT NULL,
    model_version VARCHAR(50),
    features TEXT,
    assessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_assessments_application ON risk_assessments(loan_application_id);
CREATE INDEX idx_risk_assessments_risk_band ON risk_assessments(risk_band);
