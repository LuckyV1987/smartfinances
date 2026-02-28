# FEATURE — Swagger / OpenAPI Setup

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`

Reference these existing files for context:
- `src/main/java/com/smartfinances/controller/AuthController.java`
- `src/main/java/com/smartfinances/controller/UserController.java`
- `src/main/java/com/smartfinances/dto/request/UserRequestDTO.java`
- `src/main/java/com/smartfinances/dto/request/UserUpdateRequestDTO.java`
- `src/main/java/com/smartfinances/dto/response/UserResponseDTO.java`
- `pom.xml`

---

## Type
Configuration + Dependency + Annotations

## Branch
Cut from: develop
Branch name: feature/swagger-setup
Merge to: develop via PR
Commit message: chore: add Swagger OpenAPI documentation

---

## Scope
Add Swagger UI to document all existing API endpoints. No new business logic.

---

## 1. Dependency

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

---

## 2. OpenAPI Config
**File:** `src/main/java/com/smartfinances/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartFinancesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartFinances API")
                        .description("Household budgeting application API")
                        .version("v1.0"));
    }
}
```

---

## 3. Controller Annotations

Add `@Tag` to each controller and `@Operation` to each endpoint:

### AuthController
```java
@Tag(name = "Authentication", description = "User registration and authentication")

@Operation(summary = "Register a new user", description = "Creates a new user account")
// on POST /api/auth/register
```

### UserController
```java
@Tag(name = "Users", description = "User management operations")

@Operation(summary = "Get user by ID")
// on GET /api/users/{id}

@Operation(summary = "Update user")
// on PUT /api/users/{id}

@Operation(summary = "Deactivate user")
// on DELETE /api/users/{id}
```

---

## 4. application.properties

Add:
```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

---

## Acceptance Criteria
- [ ] App starts without errors after adding dependency
- [ ] Swagger UI accessible at http://localhost:8080/swagger-ui.html
- [ ] All endpoints visible in Swagger UI
- [ ] Request and response DTOs documented correctly
- [ ] No tests required for this feature
