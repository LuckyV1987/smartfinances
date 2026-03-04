CREATE TABLE financial_accounts (
    id                  BIGSERIAL    PRIMARY KEY,
    ownership_entity_id BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    type                VARCHAR(20)  NOT NULL,
    currency_code       VARCHAR(3)   NOT NULL DEFAULT 'USD',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_financial_account_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_financial_account_entity_name
        UNIQUE (ownership_entity_id, name)
);

