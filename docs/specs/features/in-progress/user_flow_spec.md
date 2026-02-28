# FEATURE — User Registration Flow

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/branching-strategy.md`

---

## Type
DTOs + Mapper + Service + Controller

## Branch
Cut from: develop
Branch name: feature/user-registration-flow
Merge to: develop via PR
Commit message: feat: implement user registration flow controller to database

---

## Scope
Complete user registration flow from HTTP request to database persistence. No authentication or JWT in this spec — that is a separate feature.

---

## 1. DTOs

### UserRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/UserRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$",
        message = "Password must contain at least one letter and one number"
    )
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;

    private LocalDate dateOfBirth;
}
```

### UserResponseDTO
**File:** `src/main/java/com/smartfinances/dto/response/UserResponseDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
}
```

---

## 2. UserMapper
**File:** `src/main/java/com/smartfinances/mapper/UserMapper.java`

- Annotate with `@Component`
- Constructor injection only
- Two methods:
  - `toEntity(UserRequestDTO dto) → User` — hash password using BCryptPasswordEncoder
  - `toResponseDTO(User user) → UserResponseDTO`
- Never map password to ResponseDTO

---

## 3. UserService
**File:** `src/main/java/com/smartfinances/service/UserService.java`

- Annotate with `@Service`
- Constructor injection only
- Dependencies: `UserRepository`, `UserMapper`

Methods:

**register(UserRequestDTO dto) → UserResponseDTO**
1. Check if email already exists — throw `DuplicateResourceException` if so
2. Map DTO to entity via UserMapper
3. Save entity
4. Return UserResponseDTO

**findById(Long id) → UserResponseDTO**
1. Find user by id — throw `ResourceNotFoundException` if not found
2. Return UserResponseDTO

**findByEmail(String email) → User**
1. Find user by email — throw `ResourceNotFoundException` if not found
2. Return User entity (internal use only — not exposed via controller)

---

## 4. AuthController
**File:** `src/main/java/com/smartfinances/controller/AuthController.java`

- Annotate with `@RestController`
- Base mapping: `/api/auth`
- Constructor injection only
- Dependencies: `UserService`

Endpoints:

**POST /api/auth/register**
- Accepts: `@Valid @RequestBody UserRequestDTO`
- Returns: `UserResponseDTO` with HTTP 201
- Calls: `userService.register(dto)`

---

## 5. BCrypt Configuration
**File:** `src/main/java/com/smartfinances/config/SecurityConfig.java`

Add BCryptPasswordEncoder bean to existing SecurityConfig:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Inject into UserMapper via constructor.

---

## 6. Tests

### Unit Tests
**File:** `src/test/java/com/smartfinances/service/UserServiceTest.java`

Test cases:
- `shouldRegisterUser_whenValidRequest`
- `shouldThrowException_whenEmailAlreadyExists`
- `shouldReturnUser_whenValidId`
- `shouldThrowException_whenUserNotFound`
- `shouldThrowException_whenEmailNotFound`

Mock: `UserRepository`, `UserMapper`

### Integration Tests
**File:** `src/test/java/com/smartfinances/controller/AuthControllerIntegrationTest.java`

Test cases:
- `shouldReturn201_whenValidRegistration`
- `shouldReturn400_whenEmailAlreadyExists`
- `shouldReturn400_whenPasswordTooShort`
- `shouldReturn400_whenPasswordMissingNumber`
- `shouldReturn400_whenPasswordMissingLetter`
- `shouldReturn400_whenEmailInvalid`
- `shouldReturn400_whenRequiredFieldsMissing`

---

## Acceptance Criteria
- [ ] POST /api/auth/register accepts valid request and returns 201
- [ ] Password is hashed in database — never stored in plain text
- [ ] Duplicate email returns 400 with clear message
- [ ] Invalid password returns 400 with validation message
- [ ] UserResponseDTO never exposes password
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] App starts cleanly
