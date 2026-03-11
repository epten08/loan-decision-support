-- V3: Create credit_profiles table
CREATE TABLE credit_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_application_id UUID NOT NULL UNIQUE REFERENCES loan_applications(id),
    credit_score INTEGER,
    existing_loan_count INTEGER,
    total_existing_debt DECIMAL(15, 2),
    monthly_debt_payments DECIMAL(15, 2),
    credit_history_months INTEGER,
    missed_payments_last_12_months INTEGER,
    defaults_last_5_years INTEGER,
    debt_to_income_ratio DECIMAL(10, 4),
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credit_profiles_application ON credit_profiles(loan_application_id);
