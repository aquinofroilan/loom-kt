CREATE TABLE asset_transfers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    asset_id UUID NOT NULL REFERENCES fixed_assets(id),
    transfer_date DATE NOT NULL,
    from_location VARCHAR(200),
    to_location VARCHAR(200) NOT NULL,
    reason VARCHAR(1000),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE asset_revaluations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    asset_id UUID NOT NULL REFERENCES fixed_assets(id),
    revaluation_date DATE NOT NULL,
    previous_cost DECIMAL(19, 4) NOT NULL,
    new_cost DECIMAL(19, 4) NOT NULL,
    revaluation_amount DECIMAL(19, 4) NOT NULL,
    revaluation_account_id UUID NOT NULL,
    journal_entry_id UUID,
    reason VARCHAR(1000),
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
