-- V10: Add model versioning columns for ML governance

-- Add model_type to risk_assessments table
ALTER TABLE risk_assessments ADD COLUMN model_type VARCHAR(50);

-- Add model version columns to decisions table for traceability
ALTER TABLE decisions ADD COLUMN model_version VARCHAR(50);
ALTER TABLE decisions ADD COLUMN model_type VARCHAR(50);

-- Create index for model version queries
CREATE INDEX idx_risk_assessments_model_version ON risk_assessments(model_version);
CREATE INDEX idx_decisions_model_version ON decisions(model_version);
