# REFACTOR — Remove created_by and updated_by

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/migration-spec.md`

---

## Type
Refactor — Entity + Migration + Tests

## Reason
`created_by` and `updated_by` require an authenticated user to populate. These fields will be added back to individual entities when authentication is fully implemented. Removing now to avoid data integrity issues during development.

---

## Changes Required

### 1. BaseEntity
**File:** `src/main/java/com/smartfinances/entity/BaseEntity.java`

Remove these two fields:
```java
@CreatedBy
@Column(updatable = false)
private Long createdBy;

@LastModifiedBy
private Long updatedBy;
```

Keep:
```java
@CreatedDate
@Column(updatable = false)
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```

---

### 2. Users Migration
**File:** `src/main/resources/db/migration/V1__create_users_table.sql`

Remove these columns:
```sql
created_by  BIGINT NOT NULL,
updated_by  BIGINT NOT NULL
```

---

### 3. Refresh Tokens Migration
**File:** `src/main/resources/db/migration/V2__create_refresh_tokens_table.sql`

Remove these columns:
```sql
created_by  BIGINT NOT NULL,
updated_by  BIGINT NOT NULL
```

---

### 4. Tests
Review all test files and remove any references to `createdBy` or `updatedBy` fields.

---

## Important
- Flyway has already run V1 and V2 — modifying existing migration files is not allowed
- A new migration V3 must be created to drop the columns from the existing tables:

**File:** `src/main/resources/db/migration/V3__remove_created_updated_by_columns.sql`

```sql
ALTER TABLE users
    DROP COLUMN IF EXISTS created_by,
    DROP COLUMN IF EXISTS updated_by;

ALTER TABLE refresh_tokens
    DROP COLUMN IF EXISTS created_by,
    DROP COLUMN IF EXISTS updated_by;
```

---

### 5. AuditorAwareImpl
**File:** `src/main/java/com/smartfinances/config/AuditorAwareImpl.java`

Clean up duplicate code — keep only this:

```java
@Component
public class AuditorAwareImpl implements AuditorAware<Long> {
    @Override
    public Optional<Long> getCurrentAuditor() {
        // TODO: Return current authenticated user ID from SecurityContext
        return Optional.of(1L);
    }
}
```

---

## Acceptance Criteria
- [ ] BaseEntity no longer has createdBy or updatedBy fields
- [ ] V3 migration runs without errors
- [ ] Columns removed from both tables — verify in DBeaver
- [ ] AuditorAwareImpl cleaned up — no duplicate code
- [ ] No test failures after changes
- [ ] App starts cleanly after migration
