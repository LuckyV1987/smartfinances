# V1 — Create Users Table

## Agent Instructions
Before implementing this feature read and follow:
- `docs/specs/master/migration-spec.md`
- `docs/specs/master/entity-spec.md`

## Type
Database Migration + Entity + Repository

## References
- `docs/specs/master/migration-spec.md`
- `docs/specs/master/entity-spec.md`

---

## Migration

**File:** `src/main/resources/db/migration/V1__create_users_table.sql`

Create the following table:

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20),
    date_of_birth   DATE,
    deleted_at      TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    created_by      BIGINT NOT NULL,
    updated_by      BIGINT NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
```

---

## Entity

**File:** `src/main/java/com/smartfinances/entity/User.java`

- Extends `BaseEntity`
- Table name: `users`
- Soft delete via `deleted_at`
- Annotate with `@Where(clause = "deleted_at IS NULL")`
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- Write `equals()` and `hashCode()` based on `id` only

Fields:
```
Long id
String email
String password
String firstName
String lastName
String phoneNumber
LocalDate dateOfBirth
LocalDateTime deletedAt
```

---

## Repository

**File:** `src/main/java/com/smartfinances/repository/UserRepository.java`

- Extends `JpaRepository<User, Long>`
- Add method: `Optional<User> findByEmail(String email)`

---

## Acceptance Criteria
- [ ] Migration runs without errors on clean database
- [ ] Table created with all columns and correct types
- [ ] Unique constraint on email
- [ ] Index on email
- [ ] Entity maps correctly to table
- [ ] Repository findByEmail returns correct result
