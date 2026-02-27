# SmartFinances — Master Migration Spec
*Reference this in every agent session that involves creating or modifying database migrations.*

---

## Migration Tool
Flyway — runs automatically on application startup. Never modify a migration file once it has been applied.

---

## File Naming Convention

```
V{version}__{description}.sql
```

| Part | Rule | Example |
|------|------|---------|
| Version | Integer, sequential | `V1`, `V2`, `V3` |
| Separator | Double underscore | `__` |
| Description | snake_case, descriptive | `create_households_table` |

Examples:
```
V1__create_households_table.sql
V2__create_members_table.sql
V3__create_accounts_table.sql
V4__create_budget_categories_table.sql
V5__create_transactions_table.sql
V6__create_transaction_items_table.sql
```

---

## File Location

```
src/main/resources/db/migration/
```

---

## Column Standards

| Java Type | SQL Type |
|-----------|----------|
| Long (PK) | BIGSERIAL |
| Long (FK) | BIGINT |
| String | VARCHAR(255) default, adjust as needed |
| BigDecimal | NUMERIC(19,2) |
| LocalDateTime | TIMESTAMP |
| LocalDate | DATE |
| Boolean | BOOLEAN |
| Enum | VARCHAR(50) |

---

## Standard Table Template

Every table must include:

```sql
CREATE TABLE table_name (
    id          BIGSERIAL PRIMARY KEY,

    -- table specific columns here

    -- audit fields — always last
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    created_by  BIGINT NOT NULL,
    updated_by  BIGINT NOT NULL
);
```

---

## Soft Delete Tables

Tables with soft delete add `deleted_at` before audit fields:

```sql
CREATE TABLE transactions (
    id          BIGSERIAL PRIMARY KEY,

    -- table specific columns

    deleted_at  TIMESTAMP NULL,

    -- audit fields
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    created_by  BIGINT NOT NULL,
    updated_by  BIGINT NOT NULL
);
```

Soft delete tables: `transactions`, `budget_categories`

---

## Foreign Keys

Always define foreign keys explicitly:

```sql
ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_household
    FOREIGN KEY (household_id) REFERENCES households(id);
```

---

## Indexes

Add indexes on all foreign key columns and any column used in WHERE clauses:

```sql
CREATE INDEX idx_transactions_household_id ON transactions(household_id);
CREATE INDEX idx_transactions_member_id ON transactions(member_id);
CREATE INDEX idx_transactions_date ON transactions(date);
```

---

## Rules

- Never modify a migration that has already been applied
- Never use `DROP TABLE` or `DROP COLUMN` in a migration — add a new migration instead
- Always test migrations locally before committing
- One migration per logical change — don't bundle unrelated changes
- Foreign keys defined in a separate ALTER TABLE statement after table creation
- Indexes defined after foreign keys

---

## Migration Order for SmartFinances

```
V1  — create_users_table
V2  — create_refresh_tokens_table
V3  — create_households_table
V4  — create_members_table
V5  — create_accounts_table
V6  — create_budget_categories_table
V7  — create_transactions_table
V8  — create_transaction_items_table
```

Each migration depends on the previous — follow this order strictly.
User table comes first as other tables reference it via created_by, updated_by and member relationships.
