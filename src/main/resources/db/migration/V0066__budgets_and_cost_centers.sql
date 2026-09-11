CREATE TABLE cost_centers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_cost_centers_org ON cost_centers(organization_id);

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    fiscal_year_id UUID NOT NULL REFERENCES fiscal_years(id),
    name VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_budgets_org_year ON budgets(organization_id, fiscal_year_id);

CREATE TABLE budget_lines (
    id UUID PRIMARY KEY,
    budget_id UUID NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    account_id UUID REFERENCES accounts(id),
    cost_center_id UUID REFERENCES cost_centers(id),
    fiscal_period_id UUID NOT NULL REFERENCES fiscal_periods(id),
    amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    CONSTRAINT chk_budget_line_target CHECK (account_id IS NOT NULL OR cost_center_id IS NOT NULL)
);

CREATE INDEX idx_budget_lines_budget ON budget_lines(budget_id);
CREATE INDEX idx_budget_lines_account ON budget_lines(account_id);
CREATE INDEX idx_budget_lines_cost_center ON budget_lines(cost_center_id);
