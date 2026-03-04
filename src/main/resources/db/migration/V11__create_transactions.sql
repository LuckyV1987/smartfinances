CREATE TABLE transactions (
    id                  BIGSERIAL     PRIMARY KEY,
    ownership_entity_id BIGINT        NOT NULL,
    financial_account_id BIGINT       NOT NULL,
    spend_category_id   BIGINT        NOT NULL,
    budget_allocation_id BIGINT,
    type                VARCHAR(20)   NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    transaction_date    DATE          NOT NULL,
    description         VARCHAR(255),
    reference_number    VARCHAR(100),
    system_generated    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT fk_transaction_financial_account
        FOREIGN KEY (financial_account_id)
        REFERENCES financial_accounts (id),

    CONSTRAINT fk_transaction_spend_category
        FOREIGN KEY (spend_category_id)
        REFERENCES spend_categories (id),

    CONSTRAINT fk_transaction_budget_allocation
        FOREIGN KEY (budget_allocation_id)
        REFERENCES budget_allocations (id),

    -- CARRYOVER transactions must be system generated
    CONSTRAINT chk_carryover_system_generated
        CHECK (type != 'CARRYOVER' OR system_generated = TRUE),

    -- Amount must always be positive — type determines direction
    CONSTRAINT chk_positive_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_transaction_ownership_entity ON transactions (ownership_entity_id);
CREATE INDEX idx_transaction_financial_account ON transactions (financial_account_id);
CREATE INDEX idx_transaction_budget_allocation ON transactions (budget_allocation_id);
CREATE INDEX idx_transaction_date ON transactions (transaction_date);

