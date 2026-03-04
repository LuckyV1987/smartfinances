# FEATURE — Budget Allocation

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
- `src/main/java/com/smartfinances/entity/OwnershipEntity.java`
- `src/main/java/com/smartfinances/entity/SpendCategory.java`
- `src/main/java/com/smartfinances/config/SecurityConfig.java`
- `src/main/resources/db/migration/`

---

## Type
Enum + Entity + Migration + Repository + Controller + Service + Tests

## Branch
Cut from: develop
Branch name: feature/budget-allocation
Merge to: develop via PR
Commit message: feat: implement budget allocation entity, migrations and controller

---

## Scope
Budget allocations are named financial buckets owned by an OwnershipEntity.
Types: BUDGET, SAVINGS, SINKING_FUND, INCOME.
Optional target amount and optional recurrence interval.
Allocations can be one-time or recurring.

Balance is always calculated on the fly as the sum of linked transactions within
the current interval, consistently across all types. Rollover is handled by a
system-generated carryover transaction at interval start — balance calculation
never special-cases rollover. The rollover job is out of scope; the rollover flag
is the hook.

Multiple SpendCategories link to one allocation via a join table. The join table
is designed to be forward-compatible with future user-defined categories.

Uniqueness: one active allocation per name per OwnershipEntity. Soft delete only.

---

## 1. Enums

### AllocationTypeEnum
File: src/main/java/com/smartfinances/entity/enums/AllocationTypeEnum.java

```java
public enum AllocationTypeEnum {
    BUDGET,       // Spending limit — balance counts down within interval
    SAVINGS,      // Saving toward a target — balance counts up within interval
    SINKING_FUND, // Saving for a specific future expense — requires targetDate
    INCOME        // Expected inflow — tracks received income within interval
}
```

### AllocationIntervalEnum
File: src/main/java/com/smartfinances/entity/enums/AllocationIntervalEnum.java

```java
public enum AllocationIntervalEnum {
    WEEKLY,
    FORTNIGHTLY,
    MONTHLY,
    QUARTERLY,
    YEARLY
}
```

### AllocationUnitEnum
File: src/main/java/com/smartfinances/entity/enums/AllocationUnitEnum.java

```java
public enum AllocationUnitEnum {
    FIXED,      // Fixed monetary amount e.g. $500
    PERCENTAGE  // Percentage of income e.g. 0.20 = 20%
}
```

---

## 2. Entities

### BudgetAllocation
File: src/main/java/com/smartfinances/entity/BudgetAllocation.java

```java
@Entity
@Table(
    name = "budget_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_budget_allocation_entity_name",
            columnNames = {"ownership_entity_id", "name"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BudgetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_entity_id", nullable = false)
    private OwnershipEntity ownershipEntity;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AllocationTypeEnum type;

    // Spending limit, savings target, or expected inflow.
    // Interpreted per targetUnit. Null = no target set.
    @Column(name = "target_amount", precision = 19, scale = 4)
    private BigDecimal targetAmount;

    // Required when targetAmount is set.
    @Enumerated(EnumType.STRING)
    @Column(name = "target_unit", length = 20)
    private AllocationUnitEnum targetUnit;

    // Null = one-time allocation (no recurrence)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AllocationIntervalEnum interval;

    // Only applicable for SINKING_FUND
    @Column(name = "target_date")
    private LocalDate targetDate;

    // If true, a system job creates a carryover transaction at interval start.
    // Balance calculation is always: sum of transactions within current interval.
    // The carryover transaction is treated as any other transaction.
    @Column(nullable = false)
    private boolean rollover = false;

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

### BudgetAllocationCategory (Join Table)
File: src/main/java/com/smartfinances/entity/BudgetAllocationCategory.java

```java
@Entity
@Table(
    name = "budget_allocation_categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_bac_allocation_category",
            columnNames = {"budget_allocation_id", "spend_category_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BudgetAllocationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_allocation_id", nullable = false)
    private BudgetAllocation budgetAllocation;

    // System SpendCategory only.
    // Future user-defined categories use a SEPARATE join table.
    // Do NOT add user_spend_category_id to this table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spend_category_id", nullable = false)
    private SpendCategory spendCategory;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

## 3. Flyway Migrations

### V8 — Create budget_allocations
File: src/main/resources/db/migration/V8__create_budget_allocations.sql

```sql
CREATE TABLE budget_allocations (
    id                  BIGSERIAL    PRIMARY KEY,
    ownership_entity_id BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    type                VARCHAR(30)  NOT NULL,
    target_amount       NUMERIC(19, 4),
    target_unit         VARCHAR(20),
    interval            VARCHAR(20),
    target_date         DATE,
    rollover            BOOLEAN      NOT NULL DEFAULT FALSE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_budget_allocation_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_budget_allocation_entity_name
        UNIQUE (ownership_entity_id, name),

    -- SINKING_FUND must have a target_date
    CONSTRAINT chk_sinking_fund_target_date
        CHECK (type != 'SINKING_FUND' OR target_date IS NOT NULL),

    -- rollover=true requires an interval
    CONSTRAINT chk_rollover_requires_interval
        CHECK (rollover = FALSE OR interval IS NOT NULL),

    -- target_unit required when target_amount is set
    CONSTRAINT chk_target_unit_with_amount
        CHECK (target_amount IS NULL OR target_unit IS NOT NULL)
);
```

### V9 — Create budget_allocation_categories
File: src/main/resources/db/migration/V9__create_budget_allocation_categories.sql

```sql
CREATE TABLE budget_allocation_categories (
    id                   BIGSERIAL PRIMARY KEY,
    budget_allocation_id BIGINT    NOT NULL,
    spend_category_id    BIGINT    NOT NULL,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_bac_budget_allocation
        FOREIGN KEY (budget_allocation_id)
        REFERENCES budget_allocations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bac_spend_category
        FOREIGN KEY (spend_category_id)
        REFERENCES spend_categories (id),

    CONSTRAINT uq_bac_allocation_category
        UNIQUE (budget_allocation_id, spend_category_id)
);
```

---

## 4. Repositories

### BudgetAllocationRepository
File: src/main/java/com/smartfinances/repository/BudgetAllocationRepository.java

```java
@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, Long> {

    List<BudgetAllocation> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    Optional<BudgetAllocation> findByIdAndActiveTrue(Long id);

    List<BudgetAllocation> findByOwnershipEntityIdAndTypeAndActiveTrue(
        Long ownershipEntityId, AllocationTypeEnum type
    );

    boolean existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
        Long ownershipEntityId, String name
    );

    // Hook for the future rollover job
    List<BudgetAllocation> findByRolloverTrueAndActiveTrueAndIntervalIsNotNull();
}
```

### BudgetAllocationCategoryRepository
File: src/main/java/com/smartfinances/repository/BudgetAllocationCategoryRepository.java

```java
@Repository
public interface BudgetAllocationCategoryRepository
        extends JpaRepository<BudgetAllocationCategory, Long> {

    List<BudgetAllocationCategory> findByBudgetAllocationId(Long budgetAllocationId);

    boolean existsByBudgetAllocationIdAndSpendCategoryId(
        Long budgetAllocationId, Long spendCategoryId
    );

    @Transactional
    void deleteByBudgetAllocationIdAndSpendCategoryId(
        Long budgetAllocationId, Long spendCategoryId
    );
}
```

---

## 5. DTOs

### BudgetAllocationRequestDTO
File: src/main/java/com/smartfinances/dto/request/BudgetAllocationRequestDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAllocationRequestDTO {

    @NotNull(message = "Ownership entity ID is required")
    private Long ownershipEntityId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Type is required")
    private AllocationTypeEnum type;

    @DecimalMin(value = "0.0001", message = "Target amount must be greater than zero")
    private BigDecimal targetAmount;

    // Required if targetAmount is provided
    private AllocationUnitEnum targetUnit;

    // Null = one-time allocation
    private AllocationIntervalEnum interval;

    // Required if type = SINKING_FUND
    @Future(message = "Target date must be in the future")
    private LocalDate targetDate;

    private boolean rollover = false;
}
```

### BudgetAllocationResponseDTO
File: src/main/java/com/smartfinances/dto/response/BudgetAllocationResponseDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAllocationResponseDTO {
    private Long id;
    private Long ownershipEntityId;
    private String name;
    private String description;
    private AllocationTypeEnum type;
    private BigDecimal targetAmount;
    private AllocationUnitEnum targetUnit;
    private AllocationIntervalEnum interval;
    private LocalDate targetDate;
    private boolean rollover;
    private boolean active;
    private BigDecimal currentBalance;    // Calculated on the fly
    private BigDecimal remainingAmount;   // targetAmount - currentBalance; null if no target
    private List<SpendCategoryResponseDTO> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### CategoryLinkRequestDTO
File: src/main/java/com/smartfinances/dto/request/CategoryLinkRequestDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryLinkRequestDTO {

    @NotNull(message = "Spend category ID is required")
    private Long spendCategoryId;
}
```

---

## 6. Mapper

File: src/main/java/com/smartfinances/mapper/BudgetAllocationMapper.java

```java
@Component
public class BudgetAllocationMapper {

    // Maps entity + linked categories + calculated balance to response DTO.
    // remainingAmount = targetAmount - currentBalance if targetAmount is set, else null.
    public BudgetAllocationResponseDTO toResponseDTO(
        BudgetAllocation entity,
        List<SpendCategoryResponseDTO> categories,
        BigDecimal currentBalance
    ) { ... }

    // Maps request DTO to new entity.
    // Does not set ownershipEntity (resolved in service via repository lookup).
    // Does not set categories (managed via join table).
    public BudgetAllocation toEntity(BudgetAllocationRequestDTO dto) { ... }

    // Updates only non-null fields on existing entity.
    // Never updates: createdAt, ownershipEntityId, type.
    public void updateEntityFromDTO(BudgetAllocationRequestDTO dto, BudgetAllocation entity) { ... }
}
```

---

## 7. Services

SRP NOTE: Two services handle this feature.
BudgetAllocationService owns allocation lifecycle and balance calculation.
BudgetAllocationCategoryService owns category linking.
The controller depends on both.

### BudgetAllocationService
File: src/main/java/com/smartfinances/service/BudgetAllocationService.java

Constructor injection only.
Dependencies: BudgetAllocationRepository, OwnershipEntityRepository,
BudgetAllocationCategoryRepository, SpendCategoryRepository,
BudgetAllocationMapper, SpendCategoryMapper

findAllByEntity(Long ownershipEntityId) -> List<BudgetAllocationResponseDTO>
  Returns all active allocations with categories and balance
  Throws ResourceNotFoundException if entity not found or inactive

findAllByEntityAndType(Long ownershipEntityId, AllocationTypeEnum type) -> List<BudgetAllocationResponseDTO>
  Returns active allocations filtered by type

findById(Long id) -> BudgetAllocationResponseDTO
  Returns allocation with categories and balance
  Throws ResourceNotFoundException if not found or inactive

checkBalance(Long id) -> BudgetAllocationResponseDTO
  Forces fresh balance calculation
  Throws ResourceNotFoundException if not found or inactive

create(BudgetAllocationRequestDTO dto) -> BudgetAllocationResponseDTO
  Validate ownership entity exists and active -> ResourceNotFoundException
  Validate name uniqueness per entity -> DuplicateResourceException
  Apply validation rules (see below)
  Return with empty categories list and zero balance

update(Long id, BudgetAllocationRequestDTO dto) -> BudgetAllocationResponseDTO
  Throws ResourceNotFoundException if not found or inactive
  Validate name uniqueness if changed -> DuplicateResourceException
  Apply validation rules
  Non-null fields only
  type and ownershipEntityId are immutable after creation

deactivate(Long id) -> void
  Throws ResourceNotFoundException if not found or inactive
  Sets active = false

BALANCE CALCULATION RULES:
  interval is null (one-time): sum ALL linked transactions ever
  interval is set: sum transactions within current period
    e.g. MONTHLY = 1st of current month to today
  Carryover transactions included as regular transactions
  remainingAmount = targetAmount - currentBalance if targetAmount set, else null
  Returns BigDecimal.ZERO until transaction linking is implemented (out of scope)

VALIDATION RULES:
  SINKING_FUND requires targetDate -> InvalidRequestException if missing
  rollover=true requires interval -> InvalidRequestException if missing
  targetAmount provided requires targetUnit -> InvalidRequestException if missing
  PERCENTAGE unit: value must be between 0.0001 and 1.0000
  FIXED unit: value must be greater than 0

### BudgetAllocationCategoryService
File: src/main/java/com/smartfinances/service/BudgetAllocationCategoryService.java

Constructor injection only.
Dependencies: BudgetAllocationCategoryRepository, BudgetAllocationRepository,
SpendCategoryRepository, SpendCategoryMapper

findCategoriesByAllocation(Long allocationId) -> List<SpendCategoryResponseDTO>
  Throws ResourceNotFoundException if allocation not found or inactive

linkCategory(Long allocationId, Long spendCategoryId) -> List<SpendCategoryResponseDTO>
  Throws ResourceNotFoundException if allocation or category not found/inactive
  Throws DuplicateResourceException if already linked
  Returns updated full category list

unlinkCategory(Long allocationId, Long spendCategoryId) -> List<SpendCategoryResponseDTO>
  Throws ResourceNotFoundException if allocation not found or link not found
  Returns updated full category list

---

## 8. Controller

File: src/main/java/com/smartfinances/controller/BudgetAllocationController.java
Base mapping: /api/budget-allocations
@Tag(name = "Budget Allocations")
Constructor injection — depends on BudgetAllocationService and BudgetAllocationCategoryService

GET    /api/budget-allocations                             ?ownershipEntityId= (required) ?type= (optional) -> 200 List
GET    /api/budget-allocations/{id}                        -> 200, 404 if inactive
GET    /api/budget-allocations/{id}/balance                -> 200 fresh balance
POST   /api/budget-allocations                             @Valid body -> 201
PUT    /api/budget-allocations/{id}                        @Valid body -> 200
DELETE /api/budget-allocations/{id}                        -> 204 soft delete
POST   /api/budget-allocations/{id}/categories             @Valid CategoryLinkRequestDTO -> 200 List
DELETE /api/budget-allocations/{id}/categories/{catId}     -> 200 List

Security: .requestMatchers("/api/budget-allocations/**").authenticated()

---

## 9. Tests

### Unit Tests

BudgetAllocationServiceTest:
  shouldReturnAllocations_whenFindAllByEntity
  shouldReturnAllocationsByType_whenFindAllByEntityAndType
  shouldReturnAllocation_whenValidId
  shouldThrowException_whenAllocationNotFound
  shouldThrowException_whenAllocationInactive
  shouldReturnZeroBalance_whenNoTransactionsLinked
  shouldCreateAllocation_whenValidFixedRequest
  shouldCreateAllocation_whenValidPercentageRequest
  shouldCreateOneTimeAllocation_whenNoInterval
  shouldThrowException_whenSinkingFundHasNoTargetDate
  shouldThrowException_whenRolloverWithNoInterval
  shouldThrowException_whenTargetAmountHasNoUnit
  shouldThrowException_whenDuplicateNameForEntity
  shouldUpdateAllocation_whenValidRequest
  shouldNotUpdateType_whenTypeProvidedInUpdate
  shouldDeactivateAllocation_whenValidId

BudgetAllocationCategoryServiceTest:
  shouldReturnCategories_whenFindByAllocation
  shouldLinkCategory_whenValidRequest
  shouldThrowException_whenCategoryAlreadyLinked
  shouldUnlinkCategory_whenValidRequest
  shouldThrowException_whenCategoryLinkNotFound

### Integration Tests

BudgetAllocationControllerIntegrationTest:
  shouldReturn200_whenGetAllocationsByEntity
  shouldReturn200_whenGetAllocationsByType
  shouldReturn200_whenGetAllocationById
  shouldReturn200_whenCheckBalance
  shouldReturn404_whenGetInvalidAllocation
  shouldReturn404_whenGetInactiveAllocation
  shouldReturn201_whenCreateFixedAllocation
  shouldReturn201_whenCreatePercentageAllocation
  shouldReturn201_whenCreateOneTimeAllocation
  shouldReturn400_whenSinkingFundHasNoTargetDate
  shouldReturn400_whenRolloverWithNoInterval
  shouldReturn400_whenDuplicateNameForEntity
  shouldReturn200_whenUpdateAllocation
  shouldReturn204_whenDeactivateAllocation
  shouldReturn404_whenGetDeactivatedAllocation
  shouldReturn200_whenLinkCategory
  shouldReturn400_whenLinkDuplicateCategory
  shouldReturn200_whenUnlinkCategory
  shouldReturn404_whenUnlinkCategoryNotFound

---

## Acceptance Criteria
- [ ] GET ?ownershipEntityId=1 returns all active allocations
- [ ] GET ?ownershipEntityId=1&type=BUDGET filters correctly
- [ ] GET /{id}/balance returns fresh calculated balance
- [ ] POST creates FIXED, PERCENTAGE and one-time allocations
- [ ] POST rejects SINKING_FUND with no targetDate -- 400
- [ ] POST rejects rollover=true with no interval -- 400
- [ ] POST rejects targetAmount with no targetUnit -- 400
- [ ] POST rejects duplicate name for same entity -- 400
- [ ] PUT updates non-null fields only; type and ownershipEntityId never change
- [ ] DELETE soft deletes; deactivated allocation returns 404
- [ ] Category link/unlink endpoints work; duplicate link returns 400
- [ ] Linked categories returned in every allocation response
- [ ] All unit and integration tests pass

---

## Notes for Future Features
- Transaction linking: checkBalance() sums by allocation_id within current interval.
  Method signature already defined, just wire it up when transactions are implemented.
- Rollover job: findByRolloverTrueAndActiveTrueAndIntervalIsNotNull() is the hook.
  Job creates a SYSTEM carryover transaction at interval start. Balance calculation
  requires no changes.
- User-defined categories: add budget_allocation_user_categories as a SEPARATE join
  table. Do NOT modify budget_allocation_categories.
