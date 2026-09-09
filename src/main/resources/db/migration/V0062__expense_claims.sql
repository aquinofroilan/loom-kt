CREATE TABLE expense_claims (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    claim_date DATE NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL, -- DRAFT, SUBMITTED, APPROVED, REJECTED, PAID
    reimbursement_currency VARCHAR(3) NOT NULL,
    total_reimbursement_amount DECIMAL(19, 4) NOT NULL DEFAULT 0,
    workflow_instance_id UUID,
    journal_entry_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE expense_claim_lines (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL REFERENCES expense_claims(id) ON DELETE CASCADE,
    line_number INT NOT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    original_currency VARCHAR(3) NOT NULL,
    original_amount DECIMAL(19, 4) NOT NULL,
    exchange_rate DECIMAL(19, 6) NOT NULL DEFAULT 1,
    reimbursement_amount DECIMAL(19, 4) NOT NULL,
    project_id UUID,
    receipt_url VARCHAR(1000)
);

CREATE INDEX idx_expense_claims_org_emp ON expense_claims(organization_id, employee_id);
CREATE INDEX idx_expense_claim_lines_claim ON expense_claim_lines(claim_id);
