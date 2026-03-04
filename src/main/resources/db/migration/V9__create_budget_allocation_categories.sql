CREATE TABLE budget_allocation_categories (
    id                   BIGSERIAL PRIMARY KEY,
    budget_allocation_id BIGINT    NOT NULL,
    spend_category_id    BIGINT    NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bac_budget_allocation
        FOREIGN KEY (budget_allocation_id)
        REFERENCES budget_allocations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bac_spend_category
        FOREIGN KEY (spend_category_id)
        REFERENCES spend_categories (id),

    CONSTRAINT uq_bac_allocation_category
        UNIQUE (budget_allocation_id, spend_category_id)
);

