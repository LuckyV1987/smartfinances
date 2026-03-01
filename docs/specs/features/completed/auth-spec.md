# FEATURE — Auth Controller

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/branching-strategy.md`

Reference these existing files for context:
- `src/main/java/com/smartfinances/entity/User.java`
- `src/main/java/com/smartfinances/entity/RefreshToken.java`
- `src/main/java/com/smartfinances/controller/AuthController.java`
- `src/main/java/com/smartfinances/repository/UserRepository.java`
- `src/main/java/com/smartfinances/repository/RefreshTokenRepository.java`

---

## Type
Controller + Service + Tests

## Branch
Cut from: develop
Branch name: feature/auth-controller
Merge to: develop via PR
Commit message: feat: implement login, logout and token refresh endpoints

---

## Scope
Extends the existing `AuthController` which already has `POST /api/auth/register`.
Adds login, token refresh, logout, and logout-all endpoints.
JWT generation and refresh token lifecycle are handled by dedicated services.

---

## New DTOs

### LoginRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/LoginRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

### RefreshRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/RefreshRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
```

### LogoutRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/LogoutRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequestDTO {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
```

### TokenResponseDTO
**File:** `src/main/java/com/smartfinances/dto/response/TokenResponseDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;   // always "Bearer"
    private long expiresIn;     // seconds, always 900
}
```

---

## New Services

### JwtService
**File:** `src/main/java/com/smartfinances/service/JwtService.java`

Responsible for generating and validating JWT access tokens only.

**Methods:**

**generateToken(User user) → String**
- Generate a signed JWT with claims: `sub` (user UUID), `email`, `iat`, `exp`
- Access token expiry: 15 minutes (900 seconds)
- Algorithm: HS256

**validateToken(String token) → boolean**
- Returns true if token is valid and not expired
- Returns false otherwise — never throw from this method

**extractUserId(String token) → String**
- Extracts the `sub` claim from the token
- Throws `InvalidTokenException` if token is invalid

---

### RefreshTokenService
**File:** `src/main/java/com/smartfinances/service/RefreshTokenService.java`

Responsible for refresh token lifecycle management only.

**Methods:**

**createRefreshToken(User user) → RefreshToken**
1. Revoke all existing non-revoked refresh tokens for this user
2. Generate a new UUID token
3. Set expiry to 7 days from now
4. Persist and return

**validateAndRotate(String token) → RefreshToken**
1. Look up token by value — throw `InvalidTokenException` if not found
2. If revoked — throw `InvalidTokenException`
3. If expired — revoke it, throw `InvalidTokenException`
4. Revoke the current token
5. Create and return a new refresh token for the same user (calls `createRefreshToken`)

**revokeToken(String token, Long userId) → void**
1. Look up token — silently return if not found (idempotent)
2. Verify token belongs to userId — throw `ForbiddenException` if mismatch
3. Mark as revoked and save

**revokeAllForUser(Long userId) → void**
1. Find all non-revoked tokens for userId
2. Mark all as revoked and save

---

## AuthController Updates
**File:** `src/main/java/com/smartfinances/controller/AuthController.java`

- Keep existing `POST /api/auth/register`
- Add the following endpoints
- Constructor injection only
- Dependencies: `AuthService`, `JwtService`, `RefreshTokenService`

### POST /api/auth/login
- Accepts: `@Valid @RequestBody LoginRequestDTO`
- Returns: `TokenResponseDTO` with HTTP 200
- Calls: `authService.login(dto)`

### POST /api/auth/refresh
- Accepts: `@Valid @RequestBody RefreshRequestDTO`
- Returns: `TokenResponseDTO` with HTTP 200
- Calls: `authService.refresh(dto.getRefreshToken())`

### POST /api/auth/logout
- Requires: `Authorization: Bearer <token>` header
- Accepts: `@Valid @RequestBody LogoutRequestDTO`
- Returns: HTTP 204 No Content
- Calls: `authService.logout(token, dto.getRefreshToken())`

### POST /api/auth/logout-all
- Requires: `Authorization: Bearer <token>` header
- Returns: HTTP 204 No Content
- Calls: `authService.logoutAll(token)`

---

## AuthService Updates
**File:** `src/main/java/com/smartfinances/service/AuthService.java`

Add to existing `AuthService` (which already has `register`):

**login(LoginRequestDTO dto) → TokenResponseDTO**
1. Find user by email — throw `InvalidCredentialsException` if not found
2. Verify password with BCrypt — throw `InvalidCredentialsException` if mismatch
3. Check user is not deleted (deletedAt is null) — throw `AccountDeactivatedException` if deleted
4. Call `refreshTokenService.createRefreshToken(user)`
5. Call `jwtService.generateToken(user)`
6. Return `TokenResponseDTO`

Note: Always use the same generic error message for steps 1 and 2 — never reveal which field failed.

**refresh(String refreshToken) → TokenResponseDTO**
1. Call `refreshTokenService.validateAndRotate(refreshToken)` — this returns a new RefreshToken
2. Load user from new refresh token
3. Call `jwtService.generateToken(user)`
4. Return `TokenResponseDTO`

**logout(String bearerToken, String refreshToken) → void**
1. Extract and validate JWT from bearer token — throw `UnauthorizedException` if invalid
2. Extract userId from token
3. Call `refreshTokenService.revokeToken(refreshToken, userId)`

**logoutAll(String bearerToken) → void**
1. Extract and validate JWT from bearer token — throw `UnauthorizedException` if invalid
2. Extract userId from token
3. Call `refreshTokenService.revokeAllForUser(userId)`

---

## Security Config Changes
**File:** `src/main/java/com/smartfinances/config/SecurityConfig.java`

Permit without authentication:
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

Require valid JWT:
```
POST /api/auth/logout
POST /api/auth/logout-all
All other /api/** routes
```

---

## New Exceptions

- `InvalidCredentialsException` — 401, message: "Invalid email or password"
- `InvalidTokenException` — 401, message: "Token is invalid or expired"
- `AccountDeactivatedException` — 403, message: "Account is deactivated"
- `ForbiddenException` — 403, message: "Access denied"

All exceptions follow the existing error envelope format.

---

## Token Configuration
Add to `application.properties`:
```properties
jwt.secret=your-256-bit-secret-here
jwt.expiration=900
jwt.refresh-expiration=604800
```

---

## Tests

### Unit Tests

**File:** `src/test/java/com/smartfinances/service/AuthServiceTest.java`

- `shouldReturnTokenResponse_whenValidCredentials`
- `shouldThrowInvalidCredentials_whenEmailNotFound`
- `shouldThrowInvalidCredentials_whenPasswordMismatch`
- `shouldThrowAccountDeactivated_whenUserDeleted`
- `shouldReturnNewTokens_whenValidRefreshToken`
- `shouldThrowInvalidToken_whenRefreshTokenExpired`
- `shouldThrowInvalidToken_whenRefreshTokenRevoked`
- `shouldRevokeToken_whenValidLogout`
- `shouldRevokeAllTokens_whenLogoutAll`

**File:** `src/test/java/com/smartfinances/service/RefreshTokenServiceTest.java`

- `shouldCreateRefreshToken_andRevokeExisting`
- `shouldRotateToken_whenValidToken`
- `shouldThrowException_whenTokenRevoked`
- `shouldThrowException_whenTokenExpired`
- `shouldRevokeToken_whenBelongsToUser`
- `shouldThrowForbidden_whenTokenBelongsToOtherUser`
- `shouldRevokeAllTokens_forUser`

### Integration Tests

**File:** `src/test/java/com/smartfinances/controller/AuthControllerIntegrationTest.java`

- `shouldReturn200_whenLoginWithValidCredentials`
- `shouldReturn401_whenLoginWithWrongPassword`
- `shouldReturn401_whenLoginWithUnknownEmail`
- `shouldReturn403_whenLoginWithDeactivatedAccount`
- `shouldReturn200_whenRefreshWithValidToken`
- `shouldReturn401_whenRefreshWithRevokedToken`
- `shouldReturn401_whenRefreshWithExpiredToken`
- `shouldReturn204_whenLogoutWithValidToken`
- `shouldReturn401_whenLogoutWithoutAuthHeader`
- `shouldReturn204_whenLogoutAll`

---

## Acceptance Criteria
- [ ] POST /api/auth/login returns 200 with access and refresh tokens
- [ ] POST /api/auth/login returns 401 for bad credentials without revealing which field failed
- [ ] POST /api/auth/login returns 403 for deactivated accounts
- [ ] POST /api/auth/refresh returns 200 with rotated tokens
- [ ] POST /api/auth/refresh returns 401 for revoked or expired tokens
- [ ] POST /api/auth/logout returns 204 and revokes refresh token
- [ ] POST /api/auth/logout-all returns 204 and revokes all user tokens
- [ ] Existing register endpoint remains unchanged
- [ ] All unit tests pass
- [ ] All integration tests pass
