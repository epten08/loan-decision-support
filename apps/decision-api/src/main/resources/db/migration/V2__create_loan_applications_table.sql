-- V2: Create loan_applications table
CREATE TABLE loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_id UUID NOT NULL REFERENCES applicants(id),
    requested_amount DECIMAL(15, 2) NOT NULL,
    term_months INTEGER NOT NULL,
    loan_purpose VARCHAR(100) NOT NULL,
    monthly_income DECIMAL(15, 2) NOT NULL,
    monthly_expenses DECIMAL(15, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    evaluated_at TIMESTAMP
);

CREATE INDEX idx_loan_applications_applicant ON loan_applications(applicant_id);
CREATE INDEX idx_loan_applications_status ON loan_applications(status);
CREATE INDEX idx_loan_applications_created_at ON loan_applications(created_at);
