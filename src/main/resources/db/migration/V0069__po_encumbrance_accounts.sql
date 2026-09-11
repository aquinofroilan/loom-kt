ALTER TABLE purchase_order_lines
ADD COLUMN account_id UUID REFERENCES accounts(id),
ADD COLUMN cost_center_id UUID REFERENCES cost_centers(id);
