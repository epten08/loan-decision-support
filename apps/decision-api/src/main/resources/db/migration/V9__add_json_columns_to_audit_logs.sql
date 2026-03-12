-- Add JSON columns to decision_audit_logs for full reproducibility
ALTER TABLE decision_audit_logs ADD COLUMN rule_results_json TEXT;
ALTER TABLE decision_audit_logs ADD COLUMN risk_assessment_json TEXT;
