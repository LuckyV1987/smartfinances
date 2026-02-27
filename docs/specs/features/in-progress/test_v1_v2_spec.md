# TEST — V1-V2 Users and Refresh Tokens

## Agent Instructions
Before writing any tests read and follow:
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/entity-spec.md`

---

## Type
Unit Tests + Integration Tests

## References
- `docs/specs/features/completed/V1-create-users-table.md`
- `docs/specs/features/completed/V2-create-refresh-tokens-table.md`



## Tests to Write

### 1. UserRepository Integration Test
**File:** `src/test/java/com/smartfinances/repository/UserRepositoryIntegrationTest.java`

Test cases:
- `shouldFindUser_whenEmailExists`
- `shouldReturnEmpty_whenEmailNotFound`
- `shouldSaveUser_whenValidEntity`
- `shouldNotFindUser_whenSoftDeleted`

---

### 2. RefreshTokenRepository Integration Test
**File:** `src/test/java/com/smartfinances/repository/RefreshTokenRepositoryIntegrationTest.java`

Test cases:
- `shouldFindToken_whenTokenExists`
- `shouldReturnEmpty_whenTokenNotFound`
- `shouldDeleteTokens_whenUserProvided`
- `shouldSaveToken_whenValidEntity`

---

## Acceptance Criteria
- [ ] All tests pass
- [ ] No Spring context errors
- [ ] Soft delete filter verified — deleted users not returned by findByEmail
- [ ] Foreign key relationship between RefreshToken and User verified
