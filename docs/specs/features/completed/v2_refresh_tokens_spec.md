# V2 — Create Refresh Tokens Table

## Agent Instructions
Before implementing this feature read and follow:
- `docs/specs/master/migration-spec.md`
- `docs/specs/master/entity-spec.md`
- 
## Type
Database Migration + Entity + Repository

## References
- `docs/specs/master/migration-spec.md`
- `docs/specs/master/entity-spec.md`
- `docs/architecture/decisions/ADR-010-jwt-authentication.md`

## Depends On
V1 — users table must exist before this migration runs

---

## Migration

**File:** `src/main/resources/db/migration/V2__create_refresh_tokens_table.sql`

```sql
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    created_by  BIGINT NOT NULL,
    updated_by  BIGINT NOT NULL
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
```

---

## Entity

**File:** `src/main/java/com/smartfinances/entity/RefreshToken.java`

- Extends `BaseEntity`
- Table name: `refresh_tokens`
- No soft delete — refresh tokens are hard deleted on logout
- Lombok: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- Write `equals()` and `hashCode()` based on `id` only

Fields:
```
Long id
User user (@ManyToOne, FetchType.LAZY, @JoinColumn name="user_id")
String token
LocalDateTime expiresAt
```

---

## Repository

**File:** `src/main/java/com/smartfinances/repository/RefreshTokenRepository.java`

- Extends `JpaRepository<RefreshToken, Long>`
- Add methods:
  - `Optional<RefreshToken> findByToken(String token)`
  - `void deleteByUser(User user)`

---

## Acceptance Criteria
- [ ] Migration runs without errors after V1
- [ ] Foreign key to users table created correctly
- [ ] Indexes on user_id and token
- [ ] Entity maps correctly to table
- [ ] Repository methods return correct results
