CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    expense_account_id UUID NOT NULL,
    policy_limit DECIMAL(19, 4),
    limit_currency VARCHAR(3),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_expense_categories_name UNIQUE (organization_id, name)
);

-- We also need to update expense_claim_lines to link to a category ID instead of just a string category?
-- The acceptance criteria says "CRUD categories with GL mapping". If expense_claim_lines has a category ID, we can enforce limits based on it.
-- Let's check how expense_claim_lines stores category. It currently stores `category VARCHAR(100)`.
-- I can just match on the string name, or alter `expense_claim_lines` to have `category_id UUID REFERENCES expense_categories(id)`.
-- But wait, `V0062` is not merged to production yet. Can I just alter it? Yes, we can just `ALTER TABLE expense_claim_lines ADD COLUMN category_id UUID REFERENCES expense_categories(id);`
-- Let's just do `ALTER TABLE expense_claim_lines ADD COLUMN category_id UUID REFERENCES expense_categories(id);`
ALTER TABLE expense_claim_lines ADD COLUMN category_id UUID;
