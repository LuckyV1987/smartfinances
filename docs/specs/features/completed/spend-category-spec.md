# FEATURE — Spend Category Controller

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`
- `docs/specs/master/testing-spec.md`
- `docs/specs/master/branching-strategy.md`
- `docs/specs/master/repository-spec.md`
- `docs/specs/master/controller-spec.md`
- `docs/specs/master/service-spec.md`

Reference these existing files for context:
- `src/main/java/com/smartfinances/entity/User.java`
- `src/main/java/com/smartfinances/controller/AuthController.java`
- `src/main/java/com/smartfinances/config/SecurityConfig.java`
- `src/main/resources/db/migration/`

---

## Type
Entity + Migration + Controller + Service + Tests

## Branch
Cut from: develop
Branch name: feature/spend-category
Merge to: develop via PR
Commit message: feat: implement spend category entity, migrations and admin controller

---

## Scope
Implements the system-managed spend categories. These are staff-managed reference data
accessible via Swagger only. Users cannot create or modify system spend categories —
that is handled via a separate `user_spend_categories` table in a future feature.

---

## 1. Enum

**File:** `src/main/java/com/smartfinances/entity/enums/SpendCategoryType.java`

```java
public enum SpendCategoryType {
    INFLOW,
    EXPENSE
}
```

---

## 2. Entity

**File:** `src/main/java/com/smartfinances/entity/SpendCategory.java`

```java
@Entity
@Table(name = "spend_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SpendCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpendCategoryType type;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

---

## 3. Flyway Migrations

### Migration 1 — Create Table
**File:** `src/main/resources/db/migration/V4__create_spend_categories.sql`

```sql
CREATE TABLE spend_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    type        VARCHAR(20)  NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Migration 2 — Seed Default Categories
**File:** `src/main/resources/db/migration/V5__seed_spend_categories.sql`

```sql
-- INFLOW
INSERT INTO spend_categories (name, type, description) VALUES
('Salary',          'INFLOW',  'Regular employment income'),
('Freelance',       'INFLOW',  'Freelance or contract income'),
('Investment',      'INFLOW',  'Returns from investments'),
('Gift',            'INFLOW',  'Monetary gifts received'),
('Other Income',    'INFLOW',  'Any other inflow not covered above');

-- EXPENSE
INSERT INTO spend_categories (name, type, description) VALUES
('Dining',          'EXPENSE', 'Restaurants, cafes and takeaway'),
('Groceries',       'EXPENSE', 'Supermarket and food shopping'),
('Transport',       'EXPENSE', 'Fuel, public transport, ride sharing'),
('Utilities',       'EXPENSE', 'Electricity, water, internet, phone'),
('Entertainment',   'EXPENSE', 'Streaming, events, hobbies'),
('Health',          'EXPENSE', 'Medical, pharmacy, gym'),
('Education',       'EXPENSE', 'Courses, books, subscriptions'),
('Shopping',        'EXPENSE', 'Clothing, electronics, general retail'),
('Housing',         'EXPENSE', 'Rent, mortgage, maintenance'),
('Other',           'EXPENSE', 'Any other expense not covered above');
```

---

## 4. Repository

**File:** `src/main/java/com/smartfinances/repository/SpendCategoryRepository.java`

```java
@Repository
public interface SpendCategoryRepository extends JpaRepository<SpendCategory, Long> {

    List<SpendCategory> findByActiveTrue();

    List<SpendCategory> findByTypeAndActiveTrue(SpendCategoryType type);

    Optional<SpendCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
```

---

## 5. DTOs

### SpendCategoryRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/SpendCategoryRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendCategoryRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name too long")
    private String name;

    @NotNull(message = "Type is required")
    private SpendCategoryType type;

    @Size(max = 255, message = "Description too long")
    private String description;
}
```

### SpendCategoryResponseDTO
**File:** `src/main/java/com/smartfinances/dto/response/SpendCategoryResponseDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendCategoryResponseDTO {
    private Long id;
    private String name;
    private SpendCategoryType type;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 6. Mapper

**File:** `src/main/java/com/smartfinances/mapper/SpendCategoryMapper.java`

```java
@Component
public class SpendCategoryMapper {

    public SpendCategoryResponseDTO toResponseDTO(SpendCategory entity) { ... }

    public SpendCategory toEntity(SpendCategoryRequestDTO dto) { ... }

    public void updateEntityFromDTO(SpendCategoryRequestDTO dto, SpendCategory entity) {
        // Only update non-null fields
        // Never update createdAt
    }
}
```

---

## 7. Service

**File:** `src/main/java/com/smartfinances/service/SpendCategoryService.java`

- Constructor injection only
- Dependencies: `SpendCategoryRepository`, `SpendCategoryMapper`

**Methods:**

**findAll() → List\<SpendCategoryResponseDTO\>**
- Returns all active categories

**findById(Long id) → SpendCategoryResponseDTO**
- Throws `ResourceNotFoundException` if not found or inactive

**findByType(SpendCategoryType type) → List\<SpendCategoryResponseDTO\>**
- Returns active categories filtered by type

**create(SpendCategoryRequestDTO dto) → SpendCategoryResponseDTO**
- Check name uniqueness — throw `DuplicateResourceException` if exists
- Save and return

**update(Long id, SpendCategoryRequestDTO dto) → SpendCategoryResponseDTO**
- Find by id — throw `ResourceNotFoundException` if not found
- Check name uniqueness if name changed — throw `DuplicateResourceException` if exists
- Update non-null fields only
- Save and return

**deactivate(Long id) → void**
- Find by id — throw `ResourceNotFoundException` if not found
- Set `active = false`
- Save

---

## 8. Controller

**File:** `src/main/java/com/smartfinances/controller/admin/SpendCategoryController.java`

- Package: `com.smartfinances.controller.admin`
- Annotate with `@RestController`
- Base mapping: `/api/admin/spend-categories`
- Constructor injection only
- Dependencies: `SpendCategoryService`
- Tag with `@Tag(name = "Admin - Spend Categories")`

### Endpoints

**GET /api/admin/spend-categories**
- Returns: `List<SpendCategoryResponseDTO>` with HTTP 200
- Optional query param: `?type=EXPENSE` or `?type=INFLOW`
- Calls: `spendCategoryService.findAll()` or `spendCategoryService.findByType(type)`

**GET /api/admin/spend-categories/{id}**
- Returns: `SpendCategoryResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException` if not found
- Calls: `spendCategoryService.findById(id)`

**POST /api/admin/spend-categories**
- Accepts: `@Valid @RequestBody SpendCategoryRequestDTO`
- Returns: `SpendCategoryResponseDTO` with HTTP 201
- Throws: `DuplicateResourceException` if name exists
- Calls: `spendCategoryService.create(dto)`

**PUT /api/admin/spend-categories/{id}**
- Accepts: `@Valid @RequestBody SpendCategoryRequestDTO`
- Returns: `SpendCategoryResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException`, `DuplicateResourceException`
- Calls: `spendCategoryService.update(id, dto)`

**DELETE /api/admin/spend-categories/{id}**
- Returns: HTTP 204 No Content
- Throws: `ResourceNotFoundException` if not found
- Calls: `spendCategoryService.deactivate(id)`
- Soft delete — sets active = false

---

## 9. Security Config Update

Permit admin endpoints without authentication for now (Swagger staff access):

```java
.requestMatchers("/api/admin/**").permitAll()
```

---

## Tests

### Unit Tests
**File:** `src/test/java/com/smartfinances/service/SpendCategoryServiceTest.java`

- `shouldReturnAllActiveCategories_whenFindAll`
- `shouldReturnCategoriesByType_whenFindByType`
- `shouldReturnCategory_whenValidId`
- `shouldThrowException_whenCategoryNotFound`
- `shouldCreateCategory_whenValidRequest`
- `shouldThrowException_whenDuplicateName`
- `shouldUpdateCategory_whenValidRequest`
- `shouldDeactivateCategory_whenValidId`

### Integration Tests
**File:** `src/test/java/com/smartfinances/controller/admin/SpendCategoryControllerIntegrationTest.java`

- `shouldReturn200_whenGetAllCategories`
- `shouldReturn200_whenGetCategoriesByType`
- `shouldReturn200_whenGetCategoryById`
- `shouldReturn404_whenGetInvalidCategory`
- `shouldReturn201_whenCreateValidCategory`
- `shouldReturn400_whenCreateDuplicateName`
- `shouldReturn200_whenUpdateCategory`
- `shouldReturn204_whenDeactivateCategory`
- `shouldReturn404_whenGetDeactivatedCategory`

---

## Acceptance Criteria
- [ ] GET /api/admin/spend-categories returns all active categories
- [ ] GET /api/admin/spend-categories?type=EXPENSE filters correctly
- [ ] POST creates a new category and rejects duplicate names
- [ ] PUT updates only provided fields
- [ ] DELETE soft deletes — sets active = false
- [ ] Deactivated category returns 404 on subsequent GET
- [ ] Default categories visible in Swagger after Flyway migration runs
- [ ] All unit tests pass
- [ ] All integration tests pass
