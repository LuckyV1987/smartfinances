-- Add updated_at column to tables that didn't have it
-- These tables now extend BaseEntity which provides updated_at

ALTER TABLE budget_allocation_categories
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE transactions
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE transaction_details
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Note: These entities are immutable at the service layer
-- updated_at will be set on insert and never changed
-- This is acceptable per refactor-base-entity.md spec

