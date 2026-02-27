# SmartFinances — Master Testing Spec
*Reference this in every agent session that involves writing tests.*

---

## Testing Philosophy
- Test behaviour not implementation
- Every feature spec must have a corresponding test spec
- Tests are written by the agent immediately after implementation — before any human review
- You review code and tests together — never review code without tests
- Minimum 60% coverage on service layer
- Controllers and repositories are tested via integration tests only

---

## Testing Stack

| Library | Purpose |
|---------|---------|
| JUnit 5 | Test framework |
| Mockito | Mocking dependencies |
| Spring Boot Test | Integration test support |
| H2 | In-memory database for integration tests |
| AssertJ | Fluent assertions |

---

## Test Types

### Unit Tests
- Test service layer logic in isolation
- All dependencies mocked with Mockito
- No Spring context — plain Java tests
- Fast — run on every build

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldSaveUser_whenValidRequest() {
        // arrange
        // act
        // assert
    }
}
```

### Integration Tests
- Test controller → service → repository → database flow
- Uses H2 in-memory database
- Spring context loaded
- Slower — run before merge only

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterUser_whenValidRequest() throws Exception {
        // arrange
        // act — mockMvc.perform(post("/api/users/register")...)
        // assert — status, response body
    }
}
```

---

## Test Naming Convention

```
should{ExpectedBehaviour}_when{Condition}
```

Examples:
```
shouldSaveUser_whenValidRequest
shouldThrowException_whenEmailAlreadyExists
shouldReturnUser_whenValidId
shouldReturnEmpty_whenUserNotFound
shouldDeleteUser_whenValidId
```

---

## Test Structure — Arrange Act Assert

Every test follows AAA pattern with comments:

```java
@Test
void shouldSaveUser_whenValidRequest() {
    // arrange
    UserRequestDTO request = UserRequestDTO.builder()
        .email("lucky@test.com")
        .password("password123")
        .firstName("Lucky")
        .lastName("V")
        .build();

    // act
    UserResponseDTO response = userService.save(request);

    // assert
    assertThat(response.getEmail()).isEqualTo("lucky@test.com");
    assertThat(response.getId()).isNotNull();
}
```

---

## What to Test Per Layer

### Service Layer (Unit Tests)
- Happy path — valid input produces expected output
- Exception cases — invalid input throws correct exception
- Edge cases — boundary conditions, null handling
- Business logic — budget checks, validation rules

### Controller Layer (Integration Tests)
- Correct HTTP status codes returned
- Response body structure is correct
- Validation rejections return 400 with correct messages
- Auth protected endpoints return 401 when no token

### Repository Layer
- Do not unit test repositories — Spring Data JPA is already tested
- Test custom query methods via integration tests only

---

## Test File Location

```
src/test/java/com/smartfinances/
  service/
    UserServiceTest.java
    HouseholdServiceTest.java
  controller/
    UserControllerIntegrationTest.java
  repository/
    UserRepositoryIntegrationTest.java
```

---

## Mocking Rules

- Mock all external dependencies in unit tests
- Never mock the class under test
- Use `@Mock` for dependencies, `@InjectMocks` for the class under test
- Use `when().thenReturn()` for happy path
- Use `when().thenThrow()` for exception cases

```java
// correct
when(userRepository.findByEmail("lucky@test.com"))
    .thenReturn(Optional.of(existingUser));

// exception case
when(userRepository.findByEmail("lucky@test.com"))
    .thenThrow(new ResourceNotFoundException("User not found"));
```

---

## Coverage Requirements

| Layer | Minimum Coverage |
|-------|-----------------|
| Service | 60% |
| Controller | Key endpoints only |
| Repository | Custom methods only |
| Entity / DTO | Not required |

---

## What NOT to Test
- Getters and setters
- Lombok generated code
- Spring framework internals
- Simple DTOs with no logic
- Database schema correctness — covered by migration acceptance criteria
