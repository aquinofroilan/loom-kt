CREATE TABLE warehouse_zones (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (warehouse_id, code)
);

CREATE TABLE warehouse_bins (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    zone_id UUID REFERENCES warehouse_zones(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (warehouse_id, code)
);

ALTER TABLE stock_on_hand ADD COLUMN bin_id UUID REFERENCES warehouse_bins(id);

-- Replace the old constraint with the new one accommodating nullable bin_id via COALESCE
ALTER TABLE stock_on_hand DROP CONSTRAINT IF EXISTS stock_on_hand_organization_id_product_id_warehouse_id_key;
DROP INDEX IF EXISTS stock_on_hand_org_prod_wh_idx;

CREATE UNIQUE INDEX stock_on_hand_org_prod_wh_bin_idx 
ON stock_on_hand(organization_id, product_id, warehouse_id, COALESCE(bin_id, '00000000-0000-0000-0000-000000000000'::uuid));

ALTER TABLE stock_movements ADD COLUMN bin_id UUID REFERENCES warehouse_bins(id);
ALTER TABLE stock_movements ADD COLUMN transfer_to_bin_id UUID REFERENCES warehouse_bins(id);
