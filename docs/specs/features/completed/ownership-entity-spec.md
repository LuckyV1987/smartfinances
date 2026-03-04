# FEATURE — Ownership Entity & Membership

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
Enum + Entity + Migration + Repository + Controller + Service + Tests

## Branch
Cut from: develop
Branch name: feature/ownership-entity
Merge to: develop via PR
Commit message: feat: implement ownership entity and membership

---

## Scope
Implements the foundational ownership model for SmartFinances. An OwnershipEntity
is a named grouping that users belong to — either a PERSONAL space or a HOUSEHOLD.
A User can belong to many OwnershipEntities, and an OwnershipEntity can have many
Users (many-to-many via a membership join table). Every other feature that needs
an "owner" (BudgetAllocation, Transactions, Accounts etc.) will reference
ownership_entity_id rather than user_id directly, giving the app flexible
multi-owner support from day one.

Roles are out of scope for this feature — all memberships are equal for now.
A PERSONAL OwnershipEntity and its membership are auto-created when a new user
registers, handled within the existing user registration flow.

---

## 1. Enums

### OwnershipEntityType
**File:** `src/main/java/com/smartfinances/entity/enums/OwnershipEntityType.java`

```java
public enum OwnershipEntityType {
    PERSONAL,
    HOUSEHOLD
}
```

---

## 2. Entities

### OwnershipEntity
**File:** `src/main/java/com/smartfinances/entity/OwnershipEntity.java`

```java
@Entity
@Table(name = "ownership_entities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OwnershipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OwnershipEntityType type;

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

### OwnershipMembership
**File:** `src/main/java/com/smartfinances/entity/OwnershipMembership.java`

```java
@Entity
@Table(
    name = "ownership_memberships",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_ownership_membership_user_entity",
            columnNames = {"user_id", "ownership_entity_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OwnershipMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_entity_id", nullable = false)
    private OwnershipEntity ownershipEntity;

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

### Migration 1 — Create ownership_entities table
**File:** `src/main/resources/db/migration/V6__create_ownership_entities.sql`

```sql
CREATE TABLE ownership_entities (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

### Migration 2 — Create ownership_memberships table
**File:** `src/main/resources/db/migration/V7__create_ownership_memberships.sql`

```sql
CREATE TABLE ownership_memberships (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL,
    ownership_entity_id BIGINT    NOT NULL,
    active              BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES "user" (user_id),

    CONSTRAINT fk_membership_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_ownership_membership_user_entity
        UNIQUE (user_id, ownership_entity_id)
);
```

---

## 4. Repositories

### OwnershipEntityRepository
**File:** `src/main/java/com/smartfinances/repository/OwnershipEntityRepository.java`

```java
@Repository
public interface OwnershipEntityRepository extends JpaRepository<OwnershipEntity, Long> {

    List<OwnershipEntity> findByActiveTrue();

    Optional<OwnershipEntity> findByIdAndActiveTrue(Long id);

    List<OwnershipEntity> findByTypeAndActiveTrue(OwnershipEntityType type);
}
```

### OwnershipMembershipRepository
**File:** `src/main/java/com/smartfinances/repository/OwnershipMembershipRepository.java`

```java
@Repository
public interface OwnershipMembershipRepository extends JpaRepository<OwnershipMembership, Long> {

    // All active entities a user belongs to
    List<OwnershipMembership> findByUserIdAndActiveTrue(Long userId);

    // All active members of an entity
    List<OwnershipMembership> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    // Check if a specific user is already a member of an entity
    boolean existsByUserIdAndOwnershipEntityIdAndActiveTrue(Long userId, Long ownershipEntityId);

    // Find a specific membership record
    Optional<OwnershipMembership> findByUserIdAndOwnershipEntityIdAndActiveTrue(
        Long userId, Long ownershipEntityId
    );
}
```

---

## 5. DTOs

### OwnershipEntityRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/OwnershipEntityRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipEntityRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Type is required")
    private OwnershipEntityType type;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
```

### OwnershipEntityResponseDTO
**File:** `src/main/java/com/smartfinances/dto/response/OwnershipEntityResponseDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnershipEntityResponseDTO {
    private Long id;
    private String name;
    private OwnershipEntityType type;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### MembershipRequestDTO
**File:** `src/main/java/com/smartfinances/dto/request/MembershipRequestDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;
}
```

### MembershipResponseDTO
**File:** `src/main/java/com/smartfinances/dto/response/MembershipResponseDTO.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipResponseDTO {
    private Long id;
    private Long userId;
    private OwnershipEntityResponseDTO ownershipEntity;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 6. Mapper

**File:** `src/main/java/com/smartfinances/mapper/OwnershipEntityMapper.java`

```java
@Component
public class OwnershipEntityMapper {

    public OwnershipEntityResponseDTO toResponseDTO(OwnershipEntity entity) { ... }

    public OwnershipEntity toEntity(OwnershipEntityRequestDTO dto) { ... }

    // Update only non-null fields. Never update createdAt or type.
    public void updateEntityFromDTO(OwnershipEntityRequestDTO dto, OwnershipEntity entity) { ... }

    public MembershipResponseDTO toMembershipResponseDTO(OwnershipMembership membership) { ... }
}
```

---

## 7. Services

> **Single Responsibility Note:** Two separate services handle this feature.
> `OwnershipEntityService` owns entity lifecycle. `OwnershipMembershipService` owns
> membership lifecycle. Orchestration across both (e.g. signup flow) lives in
> `UserService` / `AuthService` within a `@Transactional` boundary.

### OwnershipEntityService
**File:** `src/main/java/com/smartfinances/service/OwnershipEntityService.java`

- Constructor injection only
- Dependencies: `OwnershipEntityRepository`, `OwnershipEntityMapper`

### OwnershipMembershipService
**File:** `src/main/java/com/smartfinances/service/OwnershipMembershipService.java`

- Constructor injection only
- Dependencies: `OwnershipMembershipRepository`, `OwnershipEntityRepository`,
  `UserRepository`, `OwnershipEntityMapper`

**createMembership(OwnershipEntity entity, User user) → OwnershipMembership**
- Check if active membership already exists — throw `DuplicateResourceException` if so
- Save and return membership

**addMember(Long ownershipEntityId, MembershipRequestDTO dto) → MembershipResponseDTO**
- Find entity — throw `ResourceNotFoundException` if not found or inactive
- Find user — throw `ResourceNotFoundException` if not found
- Delegate to `createMembership(entity, user)`
- Return mapped DTO

**removeMember(Long ownershipEntityId, Long userId) → void**
- Find entity — throw `ResourceNotFoundException` if not found or inactive
- Find active membership — throw `ResourceNotFoundException` if not found
- Set membership `active = false` (soft delete)
- Save

**findMembersByEntity(Long ownershipEntityId) → List\<MembershipResponseDTO\>**
- Returns all active memberships for the given entity
- Throws `ResourceNotFoundException` if entity not found or inactive

### OwnershipEntityService Methods

**findAll() → List\<OwnershipEntityResponseDTO\>**
- Returns all active ownership entities

**findById(Long id) → OwnershipEntityResponseDTO**
- Throws `ResourceNotFoundException` if not found or inactive

**findByType(OwnershipEntityType type) → List\<OwnershipEntityResponseDTO\>**
- Returns all active entities of the given type

**findEntitiesByUser(Long userId) → List\<OwnershipEntityResponseDTO\>**
- Returns all active ownership entities the user is a member of

**findMembersByEntity(Long ownershipEntityId) → List\<MembershipResponseDTO\>**
- Returns all active memberships for the given entity
- Throws `ResourceNotFoundException` if entity not found or inactive

**create(OwnershipEntityRequestDTO dto) → OwnershipEntityResponseDTO**
- Save and return new entity

**createPersonalOwnershipEntity(User user) → OwnershipEntity**
- Creates a PERSONAL OwnershipEntity named after the user (e.g. "{name}'s Personal Space")
- Does NOT create the membership — that is the responsibility of OwnershipMembershipService
- Saves and returns the entity only
- Called internally by `UserService` or `AuthService` during registration — not exposed via controller
- Orchestration of entity + membership creation is the responsibility of `UserService` / `AuthService` within a single `@Transactional` boundary

**update(Long id, OwnershipEntityRequestDTO dto) → OwnershipEntityResponseDTO**
- Find by id — throw `ResourceNotFoundException` if not found or inactive
- Update non-null fields only. Type cannot be changed after creation.
- Save and return

**deactivate(Long id) → void**
- Find by id — throw `ResourceNotFoundException` if not found or inactive
- Set `active = false`
- Save



---

## 8. Controller

**File:** `src/main/java/com/smartfinances/controller/OwnershipEntityController.java`

- Package: `com.smartfinances.controller`
- Annotate with `@RestController`
- Base mapping: `/api/ownership-entities`
- Constructor injection only
- Dependencies: `OwnershipEntityService`
- Tag with `@Tag(name = "Ownership Entities")`

### Endpoints

**GET /api/ownership-entities**
- Optional query param: `?type=PERSONAL` or `?type=HOUSEHOLD`
- Returns: `List<OwnershipEntityResponseDTO>` with HTTP 200
- Calls: `service.findAll()` or `service.findByType(type)`

**GET /api/ownership-entities/{id}**
- Returns: `OwnershipEntityResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException` if not found or inactive
- Calls: `service.findById(id)`

**GET /api/ownership-entities/user/{userId}**
- Returns: `List<OwnershipEntityResponseDTO>` with HTTP 200
- Returns all entities the user is an active member of
- Calls: `service.findEntitiesByUser(userId)`

**GET /api/ownership-entities/{id}/members**
- Returns: `List<MembershipResponseDTO>` with HTTP 200
- Throws: `ResourceNotFoundException` if entity not found or inactive
- Calls: `service.findMembersByEntity(id)`

**POST /api/ownership-entities**
- Accepts: `@Valid @RequestBody OwnershipEntityRequestDTO`
- Returns: `OwnershipEntityResponseDTO` with HTTP 201
- Calls: `service.create(dto)`

**PUT /api/ownership-entities/{id}**
- Accepts: `@Valid @RequestBody OwnershipEntityRequestDTO`
- Returns: `OwnershipEntityResponseDTO` with HTTP 200
- Throws: `ResourceNotFoundException`
- Calls: `service.update(id, dto)`

**DELETE /api/ownership-entities/{id}**
- Returns: HTTP 204 No Content
- Throws: `ResourceNotFoundException` if not found or inactive
- Calls: `service.deactivate(id)`
- Soft delete — sets active = false

**POST /api/ownership-entities/{id}/members**
- Accepts: `@Valid @RequestBody MembershipRequestDTO`
- Returns: `MembershipResponseDTO` with HTTP 201
- Throws: `ResourceNotFoundException`, `DuplicateResourceException`
- Calls: `service.addMember(id, dto)`

**DELETE /api/ownership-entities/{id}/members/{userId}**
- Returns: HTTP 204 No Content
- Throws: `ResourceNotFoundException` if entity or membership not found
- Calls: `service.removeMember(id, userId)`
- Soft delete — sets membership active = false

---

## 9. Security Config Update

Permit ownership entity endpoints for authenticated users:

```java
.requestMatchers("/api/ownership-entities/**").authenticated()
```

---

## Tests

### Unit Tests
**File:** `src/test/java/com/smartfinances/service/OwnershipEntityServiceTest.java`

- `shouldCreatePersonalOwnershipEntity_whenNewUserRegisters`
- `shouldReturnAllActiveEntities_whenFindAll`
- `shouldReturnEntitiesByType_whenFindByType`
- `shouldReturnEntity_whenValidId`
- `shouldThrowException_whenEntityNotFound`
- `shouldThrowException_whenEntityInactive`
- `shouldReturnEntities_whenFindByUser`
- `shouldReturnMembers_whenFindByEntity`
- `shouldCreateEntity_whenValidRequest`
- `shouldUpdateEntity_whenValidRequest`
- `shouldNotUpdateType_whenTypeProvidedInUpdate`
- `shouldDeactivateEntity_whenValidId`
- `shouldAddMember_whenValidRequest`
- `shouldThrowException_whenMemberAlreadyExists`
- `shouldRemoveMember_whenValidRequest`
- `shouldThrowException_whenMembershipNotFound`

### Integration Tests
**File:** `src/test/java/com/smartfinances/controller/OwnershipEntityControllerIntegrationTest.java`

- `shouldReturn200_whenGetAllEntities`
- `shouldReturn200_whenGetEntitiesByType`
- `shouldReturn200_whenGetEntityById`
- `shouldReturn404_whenGetInvalidEntity`
- `shouldReturn404_whenGetInactiveEntity`
- `shouldReturn200_whenGetEntitiesByUser`
- `shouldReturn200_whenGetMembersByEntity`
- `shouldReturn201_whenCreateEntity`
- `shouldReturn200_whenUpdateEntity`
- `shouldReturn204_whenDeactivateEntity`
- `shouldReturn404_whenGetDeactivatedEntity`
- `shouldReturn201_whenAddMember`
- `shouldReturn400_whenAddDuplicateMember`
- `shouldReturn204_whenRemoveMember`
- `shouldReturn404_whenRemoveMemberNotFound`

---

## Acceptance Criteria
- [ ] GET /api/ownership-entities returns all active entities
- [ ] GET /api/ownership-entities?type=PERSONAL filters correctly
- [ ] GET /api/ownership-entities/user/{userId} returns all entities the user belongs to
- [ ] GET /api/ownership-entities/{id}/members returns all active members
- [ ] POST creates a new ownership entity
- [ ] PUT updates name and description — type cannot be changed
- [ ] DELETE soft deletes — sets active = false
- [ ] Deactivated entity returns 404 on subsequent GET
- [ ] POST /{id}/members adds a user to an entity
- [ ] POST /{id}/members rejects duplicate membership with 400
- [ ] DELETE /{id}/members/{userId} soft deletes the membership
- [ ] All unit tests pass
- [ ] All integration tests pass

---

## Notes for Future Features
- BudgetAllocation, Transactions, and Accounts should reference `ownership_entity_id`
  (FK to `ownership_entities.id`) rather than `user_id` directly
- Auto-creation of PERSONAL entities on signup is in scope — hook `createPersonalOwnershipEntity()`
  into the User registration flow in `AuthController` / `UserService`
- Role-based membership (ADMIN, MEMBER, VIEWER) can be added by introducing a
  `role` column on `ownership_memberships` without breaking this schema
