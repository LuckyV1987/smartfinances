CREATE TABLE budget_allocations (
    id                  BIGSERIAL    PRIMARY KEY,
    ownership_entity_id BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    type                VARCHAR(30)  NOT NULL,
    target_amount       NUMERIC(19, 4),
    target_unit         VARCHAR(20),
    allocation_interval VARCHAR(20),
    target_date         DATE,
    rollover            BOOLEAN      NOT NULL DEFAULT FALSE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_budget_allocation_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_budget_allocation_entity_name
        UNIQUE (ownership_entity_id, name),

    -- SINKING_FUND must have a target_date
    CONSTRAINT chk_sinking_fund_target_date
        CHECK (type != 'SINKING_FUND' OR target_date IS NOT NULL),

    -- rollover=true requires an allocation_interval
    CONSTRAINT chk_rollover_requires_interval
        CHECK (rollover = FALSE OR allocation_interval IS NOT NULL),

    -- target_unit required when target_amount is set
    CONSTRAINT chk_target_unit_with_amount
        CHECK (target_amount IS NULL OR target_unit IS NOT NULL)
);

