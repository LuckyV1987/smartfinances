# FEATURE — RefreshTokenRepository

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/branching-strategy.md`

Reference these existing files for context:
- `src/main/java/com/smartfinances/entity/RefreshToken.java`
- `src/main/java/com/smartfinances/entity/User.java`
- `src/main/java/com/smartfinances/repository/RefreshTokenRepository.java`
- `src/main/java/com/smartfinances/service/RefreshTokenService.java`
- `src/main/java/com/smartfinances/exception/InvalidTokenException.java`
- `src/main/java/com/smartfinances/exception/ForbiddenException.java`

---

## Type
Repository + Tests

## Branch
Cut from: feature/auth-controller
Branch name: fix/refresh-token-repository
Merge to: feature/auth-controller via PR
Commit message: fix: add missing RefreshTokenRepository query methods

---

## Scope
Add missing derived query methods to `RefreshTokenRepository` to support efficient database-level operations. Removes the inefficient in-memory filtering currently in `RefreshTokenService.revokeAllForUser`.

---

## RefreshTokenRepository
**File:** `src/main/java/com/smartfinances/repository/RefreshTokenRepository.java`

```java
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByUserId(Long userId);
}
```

---

## RefreshTokenService Fix
**File:** `src/main/java/com/smartfinances/service/RefreshTokenService.java`

Replace the current `revokeAllForUser` implementation:

**Before (inefficient — loads all tokens into memory):**
```java
public void revokeAllForUser(Long userId) {
    List<RefreshToken> tokens = refreshTokenRepository.findAll().stream()
        .filter(t -> t.getUser().getId().equals(userId))
        .toList();
    refreshTokenRepository.deleteAll(tokens);
}
```

**After (single DB call):**
```java
@Transactional
public void revokeAllForUser(Long userId) {
    refreshTokenRepository.deleteByUserId(userId);
}
```

---

## Tests

### Unit Tests
**File:** `src/test/java/com/smartfinances/service/RefreshTokenServiceTest.java`

- `shouldDeleteAllTokens_whenRevokeAllForUser`
- `shouldCallRepositoryOnce_whenRevokeAllForUser`

### Repository Tests
**File:** `src/test/java/com/smartfinances/repository/RefreshTokenRepositoryTest.java`

- `shouldFindToken_whenValidTokenString`
- `shouldReturnEmpty_whenTokenNotFound`
- `shouldDeleteByUser_whenUserHasTokens`
- `shouldDeleteByUserId_whenUserHasMultipleTokens`
- `shouldNotDeleteOtherUserTokens_whenDeletingByUserId`

---

## Acceptance Criteria
- [ ] `findByToken` returns correct token or empty
- [ ] `deleteByUser` removes all tokens for a given User entity
- [ ] `deleteByUserId` removes all tokens for a given user ID
- [ ] `revokeAllForUser` no longer loads all tokens into memory
- [ ] No other tokens are affected when deleting by user ID
- [ ] All tests pass
