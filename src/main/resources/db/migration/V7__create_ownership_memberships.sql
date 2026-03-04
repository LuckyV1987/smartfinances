CREATE TABLE ownership_memberships (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL,
    ownership_entity_id BIGINT    NOT NULL,
    active              BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),

    CONSTRAINT fk_membership_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_ownership_membership_user_entity
        UNIQUE (user_id, ownership_entity_id)
);

