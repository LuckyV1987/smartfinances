# FEATURE — User Controller

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/branching-strategy.md`

Reference these existing files for context:
- `src/main/java/com/smartfinances/entity/User.java`
- `src/main/java/com/smartfinances/service/UserService.java`
- `src/main/java/com/smartfinances/mapper/UserMapper.java`
- `src/main/java/com/smartfinances/dto/response/UserResponseDTO.java`

---

## Type
Controller + Service methods + Tests

## Branch
Cut from: develop
Branch name: feature/user-controller
Merge to: develop via PR
Commit message: feat: implement user controller with get, update and deactivate

---

## Scope
User management endpoints. Authentication is handled separately in AuthController. All endpoints in this controller require authentication — security will be enforced when JWT is implemented.

---

## Additional DTOs Needed

### UserUpdateRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/UserUpdateRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name too long")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name too long")
    private String lastName;

    @Size(max = 20, message = "Phone number too long")
    private String phoneNumber;

    private LocalDate dateOfBirth;
}
```

First name and last name are required. Phone number and date of birth are optional.

---

## UserController
**File:** `src/main/java/com/smartfinances/controller/UserController.java`

- Annotate with `@RestController`
- Base mapping: `/api/users`
- Constructor injection only
- Dependencies: `UserService`

### Endpoints

**GET /api/users/{id}**
- Returns: `UserResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException` if user not found
- Calls: `userService.findById(id)`

**PUT /api/users/{id}**
- Accepts: `@RequestBody UserUpdateRequestDTO`
- Returns: `UserResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException` if user not found
- Calls: `userService.update(id, dto)`
- Only updates fields that are not null in the request

**DELETE /api/users/{id}**
- Returns: HTTP 204 No Content
- Throws: `ResourceNotFoundException` if user not found
- Calls: `userService.deactivate(id)`
- Soft delete — sets deletedAt, does not hard delete

---

## Additional UserService Methods

Add to existing `UserService`:

**update(Long id, UserUpdateRequestDTO dto) → UserResponseDTO**
1. Find user by id — throw `ResourceNotFoundException` if not found
2. Update only non-null fields from dto
3. Save and return UserResponseDTO

**deactivate(Long id) → void**
1. Find user by id — throw `ResourceNotFoundException` if not found
2. Set `deletedAt = LocalDateTime.now()`
3. Save user

---

## Additional UserMapper Method

Add to existing `UserMapper`:

**updateEntityFromDTO(UserUpdateRequestDTO dto, User user) → void**
- Only update fields that are not null in dto
- Never update email or password via this method

---

## Tests

### Unit Tests
**File:** `src/test/java/com/smartfinances/service/UserServiceTest.java`

Add test cases:
- `shouldUpdateUser_whenValidRequest`
- `shouldOnlyUpdateProvidedFields_whenPartialRequest`
- `shouldThrowException_whenUserNotFoundOnUpdate`
- `shouldDeactivateUser_whenValidId`
- `shouldThrowException_whenUserNotFoundOnDeactivate`

### Integration Tests
**File:** `src/test/java/com/smartfinances/controller/UserControllerIntegrationTest.java`

Test cases:
- `shouldReturn200_whenGetUserByValidId`
- `shouldReturn404_whenGetUserByInvalidId`
- `shouldReturn200_whenUpdateUserWithValidRequest`
- `shouldReturn404_whenUpdateUserWithInvalidId`
- `shouldOnlyUpdateProvidedFields_whenPartialUpdateRequest`
- `shouldReturn204_whenDeactivateValidUser`
- `shouldReturn404_whenDeactivateInvalidUser`
- `shouldReturn404_whenGetDeactivatedUser`

---

## Acceptance Criteria
- [ ] GET /api/users/{id} returns 200 with correct user data
- [ ] GET /api/users/{id} returns 404 for unknown id
- [ ] PUT /api/users/{id} updates only provided fields
- [ ] PUT /api/users/{id} never exposes or updates password
- [ ] DELETE /api/users/{id} soft deletes — sets deletedAt
- [ ] Deactivated user returns 404 on subsequent GET
- [ ] All unit tests pass
- [ ] All integration tests pass
