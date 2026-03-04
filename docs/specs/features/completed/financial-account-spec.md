# FEATURE — Financial Account

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
- `src/main/java/com/smartfinances/config/SecurityConfig.java`
- `src/main/resources/db/migration/`

---

## Type
Enum + Entity + Migration + Repository + Controller + Service + Tests

## Branch
Cut from: develop
Branch name: feature/financial-account
Merge to: develop via PR
Commit message: feat: implement financial account entity, migrations and controller

---

## Scope
Financial accounts represent real-world accounts owned by an OwnershipEntity.
Supported types: CHECKING, SAVINGS, CREDIT_CARD, CASH, INVESTMENT.

Balance is calculated on the fly from linked transactions. A stored balance field
is not maintained — the balance method returns the sum of all transactions for the
account.

Single currency per account for now. Currency code is stored on the account record
so multi-currency support can be added later without a schema change.

Soft delete only. Uniqueness: one active account per name per OwnershipEntity.

---

## 1. Enum

### AccountTypeEnum
File: src/main/java/com/smartfinances/entity/enums/AccountTypeEnum.java

```java
public enum AccountTypeEnum {
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    CASH,
    INVESTMENT
}
```

---

## 2. Entity

### FinancialAccount
File: src/main/java/com/smartfinances/entity/FinancialAccount.java

```java
@Entity
@Table(
    name = "financial_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_financial_account_entity_name",
            columnNames = {"ownership_entity_id", "name"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FinancialAccount {

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
    @Column(nullable = false, length = 20)
    private AccountTypeEnum type;

    // ISO 4217 currency code e.g. USD, AUD.
    // Single currency per account for now.
    // Stored here so multi-currency support requires no schema change.
    @Column(nullable = false, length = 3)
    private String currencyCode = "USD";

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

## 3. Flyway Migration

### V10 — Create financial_accounts
File: src/main/resources/db/migration/V10__create_financial_accounts.sql

```sql
CREATE TABLE financial_accounts (
    id                  BIGSERIAL    PRIMARY KEY,
    ownership_entity_id BIGINT       NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(255),
    type                VARCHAR(20)  NOT NULL,
    currency_code       VARCHAR(3)   NOT NULL DEFAULT 'USD',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_financial_account_ownership_entity
        FOREIGN KEY (ownership_entity_id)
        REFERENCES ownership_entities (id),

    CONSTRAINT uq_financial_account_entity_name
        UNIQUE (ownership_entity_id, name)
);
```

---

## 4. Repository

### FinancialAccountRepository
File: src/main/java/com/smartfinances/repository/FinancialAccountRepository.java

```java
@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, Long> {

    List<FinancialAccount> findByOwnershipEntityIdAndActiveTrue(Long ownershipEntityId);

    Optional<FinancialAccount> findByIdAndActiveTrue(Long id);

    List<FinancialAccount> findByOwnershipEntityIdAndTypeAndActiveTrue(
        Long ownershipEntityId, AccountTypeEnum type
    );

    boolean existsByOwnershipEntityIdAndNameIgnoreCaseAndActiveTrue(
        Long ownershipEntityId, String name
    );
}
```

---

## 5. DTOs

### FinancialAccountRequestDTO
File: src/main/java/com/smartfinances/dto/request/FinancialAccountRequestDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAccountRequestDTO {

    @NotNull(message = "Ownership entity ID is required")
    private Long ownershipEntityId;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Account type is required")
    private AccountTypeEnum type;

    // Defaults to USD if not provided
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String currencyCode = "USD";
}
```

### FinancialAccountResponseDTO
File: src/main/java/com/smartfinances/dto/response/FinancialAccountResponseDTO.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialAccountResponseDTO {
    private Long id;
    private Long ownershipEntityId;
    private String name;
    private String description;
    private AccountTypeEnum type;
    private String currencyCode;
    private boolean active;
    private BigDecimal currentBalance;  // Calculated on the fly from transactions
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 6. Mapper

File: src/main/java/com/smartfinances/mapper/FinancialAccountMapper.java

```java
@Component
public class FinancialAccountMapper {

    // Maps entity + calculated balance to response DTO.
    public FinancialAccountResponseDTO toResponseDTO(
        FinancialAccount entity,
        BigDecimal currentBalance
    ) { ... }

    // Maps request DTO to new entity.
    // Does not set ownershipEntity (resolved in service).
    public FinancialAccount toEntity(FinancialAccountRequestDTO dto) { ... }

    // Updates only non-null fields.
    // Never updates: createdAt, ownershipEntityId, type, currencyCode.
    public void updateEntityFromDTO(FinancialAccountRequestDTO dto, FinancialAccount entity) { ... }
}
```

---

## 7. Service

### FinancialAccountService
File: src/main/java/com/smartfinances/service/FinancialAccountService.java

Constructor injection only.
Dependencies: FinancialAccountRepository, OwnershipEntityRepository,
FinancialAccountMapper

findAllByEntity(Long ownershipEntityId) -> List<FinancialAccountResponseDTO>
  Returns all active accounts for the given ownership entity with calculated balance
  Throws ResourceNotFoundException if entity not found or inactive

findAllByEntityAndType(Long ownershipEntityId, AccountTypeEnum type) -> List<FinancialAccountResponseDTO>
  Returns active accounts filtered by type

findById(Long id) -> FinancialAccountResponseDTO
  Returns account with calculated balance
  Throws ResourceNotFoundException if not found or inactive

getBalance(Long id) -> FinancialAccountResponseDTO
  Forces fresh balance calculation
  Throws ResourceNotFoundException if not found or inactive

create(FinancialAccountRequestDTO dto) -> FinancialAccountResponseDTO
  Validate ownership entity exists and active -> ResourceNotFoundException
  Validate name uniqueness per entity -> DuplicateResourceException
  currencyCode defaults to USD if not provided
  Return with zero balance

update(Long id, FinancialAccountRequestDTO dto) -> FinancialAccountResponseDTO
  Throws ResourceNotFoundException if not found or inactive
  Validate name uniqueness if changed -> DuplicateResourceException
  Non-null fields only
  type, ownershipEntityId, currencyCode are immutable after creation
  Plaid fields are never updated via this method

deactivate(Long id) -> void
  Throws ResourceNotFoundException if not found or inactive
  Sets active = false

BALANCE CALCULATION:
  Sum of all linked transactions for this account
  Returns BigDecimal.ZERO until transaction linking is implemented (out of scope)

---

## 8. Controller

File: src/main/java/com/smartfinances/controller/FinancialAccountController.java
Base mapping: /api/financial-accounts
@Tag(name = "Financial Accounts")
Constructor injection — depends on FinancialAccountService only

GET    /api/financial-accounts                         ?ownershipEntityId= (required) ?type= (optional) -> 200 List
GET    /api/financial-accounts/{id}                    -> 200, 404 if inactive
GET    /api/financial-accounts/{id}/balance            -> 200 fresh balance
POST   /api/financial-accounts                         @Valid body -> 201
PUT    /api/financial-accounts/{id}                    @Valid body -> 200
DELETE /api/financial-accounts/{id}                    -> 204 soft delete

Security: .requestMatchers("/api/financial-accounts/**").authenticated()

---

## 9. Tests

### Unit Tests

FinancialAccountServiceTest:
  shouldReturnAccounts_whenFindAllByEntity
  shouldReturnAccountsByType_whenFindAllByEntityAndType
  shouldReturnAccount_whenValidId
  shouldThrowException_whenAccountNotFound
  shouldThrowException_whenAccountInactive
  shouldReturnZeroBalance_whenNoTransactionsLinked
  shouldCreateAccount_whenValidRequest
  shouldDefaultCurrencyToUSD_whenNotProvided
  shouldThrowException_whenDuplicateNameForEntity
  shouldUpdateAccount_whenValidRequest
  shouldNotUpdateType_whenTypeProvidedInUpdate
  shouldNotUpdateCurrencyCode_whenProvidedInUpdate
  shouldDeactivateAccount_whenValidId

### Integration Tests

FinancialAccountControllerIntegrationTest:
  shouldReturn200_whenGetAccountsByEntity
  shouldReturn200_whenGetAccountsByType
  shouldReturn200_whenGetAccountById
  shouldReturn200_whenGetBalance
  shouldReturn404_whenGetInvalidAccount
  shouldReturn404_whenGetInactiveAccount
  shouldReturn201_whenCreateAccount
  shouldReturn201_whenCreateAccountWithDefaultCurrency
  shouldReturn400_whenDuplicateNameForEntity
  shouldReturn200_whenUpdateAccount
  shouldReturn204_whenDeactivateAccount
  shouldReturn404_whenGetDeactivatedAccount

---

## Acceptance Criteria
- [ ] GET ?ownershipEntityId=1 returns all active accounts
- [ ] GET ?ownershipEntityId=1&type=CHECKING filters correctly
- [ ] GET /{id}/balance returns fresh calculated balance
- [ ] POST creates account with explicit currency code
- [ ] POST defaults currency to USD when not provided
- [ ] POST rejects duplicate name for same entity -- 400
- [ ] PUT updates non-null fields only
- [ ] PUT never changes type, currencyCode, or Plaid fields
- [ ] DELETE soft deletes; deactivated account returns 404
- [ ] All unit and integration tests pass

---

## Notes for Future Features
- Transaction linking: getBalance() sums transactions by account_id.
  Method signature already defined, wire it up when transactions are implemented.
- Bank sync (Plaid): Add a separate bank-sync feature spec when ready.
  Schema will need: external_account_id, institution_name, last_synced_at columns.
  Build as a separate migration on top of this feature.
- Multi-currency: currencyCode is already stored per account. When multi-currency
  arrives add a conversion service — no schema change needed.
