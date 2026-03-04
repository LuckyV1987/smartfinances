CREATE TABLE transaction_details (
    id             BIGSERIAL      PRIMARY KEY,
    transaction_id BIGINT         NOT NULL,
    name           VARCHAR(100)   NOT NULL,
    quantity       NUMERIC(10,3)  NOT NULL,
    unit           VARCHAR(20),
    unit_price     NUMERIC(19,4)  NOT NULL,
    total_price    NUMERIC(19,4)  NOT NULL,
    notes          VARCHAR(255),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_detail_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions (id)
        ON DELETE CASCADE,

    -- total_price must equal quantity * unit_price (within rounding tolerance)
    CONSTRAINT chk_total_price_consistency
        CHECK (ABS(total_price - (quantity * unit_price)) < 0.01)
);

CREATE INDEX idx_transaction_detail_transaction ON transaction_details (transaction_id);

