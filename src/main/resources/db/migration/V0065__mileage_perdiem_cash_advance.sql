-- Add type, quantity, unit price to expense_claim_lines
ALTER TABLE expense_claim_lines 
ADD COLUMN line_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
ADD COLUMN quantity DECIMAL(19, 4),
ADD COLUMN unit_price DECIMAL(19, 4);

-- Create cash_advances table
CREATE TABLE cash_advances (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    date DATE NOT NULL,
    purpose VARCHAR(1000) NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    journal_entry_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Add cash_advance_id and applied_advance_amount to expense_claims
ALTER TABLE expense_claims
ADD COLUMN cash_advance_id UUID REFERENCES cash_advances(id),
ADD COLUMN applied_advance_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.00;

