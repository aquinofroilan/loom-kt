CREATE TABLE forecasts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE forecast_lines (
    id UUID PRIMARY KEY,
    forecast_id UUID NOT NULL REFERENCES forecasts(id) ON DELETE CASCADE,
    account_id UUID,
    cost_center_id UUID,
    fiscal_period_id UUID NOT NULL REFERENCES fiscal_periods(id),
    amount DECIMAL(19, 4) NOT NULL DEFAULT 0
);
