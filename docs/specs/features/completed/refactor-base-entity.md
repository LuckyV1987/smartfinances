# REFACTOR — Wire BaseEntity Across All Entities

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/entity-spec.md`

Reference these existing files:
- `src/main/java/com/smartfinances/entity/BaseEntity.java`
- All entity files in `src/main/java/com/smartfinances/entity/`

---

## Type
Refactor — no new features, no schema changes, no migrations

## Note
No branch required — apply directly to current working state.
No database changes. No Flyway migrations. Pure Java class restructure.

---

## Background

BaseEntity exists but no entity is extending it. Every entity currently
duplicates createdAt and updatedAt fields along with their audit annotations
and @EntityListeners. This refactor wires all entities to BaseEntity,
removing the duplication.

BaseEntity already has:
- @MappedSuperclass
- @EntityListeners(AuditingEntityListener.class)
- @CreatedDate createdAt
- @LastModifiedDate updatedAt
- Manual getters and setters

BaseEntity does NOT have id — id stays on each entity as generation
strategies may vary.

---

## Rules for Every Entity

For EVERY entity listed below, make these three changes and nothing else:

1. Add `extends BaseEntity` to the class declaration
2. Remove the `createdAt` field and all its annotations (@CreatedDate, @Column)
3. Remove the `updatedAt` field and all its annotations (@LastModifiedDate, @Column)
   NOTE: Some entities like TransactionDetail and Transaction have no updatedAt
   by design (immutable). For those entities only remove createdAt.
4. Remove `@EntityListeners(AuditingEntityListener.class)` from the entity
   (it is already on BaseEntity)

Do NOT change:
- id field or its annotations
- Any other fields
- Lombok annotations
- equals/hashCode implementations
- Table annotations or constraints
- Any business logic

---

## Entities to Refactor

### 1. User
File: src/main/java/com/smartfinances/entity/User.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 2. OwnershipEntity
File: src/main/java/com/smartfinances/entity/OwnershipEntity.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 3. OwnershipMembership
File: src/main/java/com/smartfinances/entity/OwnershipMembership.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 4. SpendCategory
File: src/main/java/com/smartfinances/entity/SpendCategory.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 5. BudgetAllocation
File: src/main/java/com/smartfinances/entity/BudgetAllocation.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 6. BudgetAllocationCategory
File: src/main/java/com/smartfinances/entity/BudgetAllocationCategory.java
Remove: createdAt, @EntityListeners
NOTE: This entity has createdAt only — no updatedAt by design (join table).
Add: extends BaseEntity
NOTE: BaseEntity has updatedAt which will now be present on this table.
See BaseEntity note below.

### 7. FinancialAccount
File: src/main/java/com/smartfinances/entity/FinancialAccount.java
Remove: createdAt, updatedAt, @EntityListeners
Add: extends BaseEntity

### 8. Transaction
File: src/main/java/com/smartfinances/entity/Transaction.java
Remove: createdAt, @EntityListeners
NOTE: Transaction has createdAt only — no updatedAt (immutable by design).
Add: extends BaseEntity
NOTE: See BaseEntity note below regarding updatedAt.

### 9. TransactionDetail
File: src/main/java/com/smartfinances/entity/TransactionDetail.java
Remove: createdAt, @EntityListeners
NOTE: TransactionDetail has createdAt only — no updatedAt (immutable by design).
Add: extends BaseEntity
NOTE: See BaseEntity note below regarding updatedAt.

---

## Important Note — updatedAt on Immutable Entities

Transaction, TransactionDetail, and BudgetAllocationCategory currently have
createdAt only. By extending BaseEntity they will inherit updatedAt as well.

This is ACCEPTABLE because:
- Immutability is enforced at the service layer (no update methods exist)
- The updatedAt column will be set on insert and never touched again
- No business logic reads updatedAt on these entities
- The alternative (a separate BaseCreatedEntity without updatedAt) adds
  unnecessary complexity for marginal gain

Do NOT create a separate base class to avoid this. Keep it simple.

---

## Result After Refactor

Every entity class declaration will look like:

```java
@Entity
@Table(name = "...")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SomeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // domain-specific fields only
    // no createdAt, no updatedAt, no @EntityListeners
}
```

---

## Imports to Remove from Each Entity

Remove these imports from every entity after the refactor:

  import org.springframework.data.annotation.CreatedDate;
  import org.springframework.data.annotation.LastModifiedDate;
  import org.springframework.data.jpa.domain.support.AuditingEntityListener;
  import java.time.LocalDateTime;  -- only if no other LocalDateTime fields remain

---

## Verification Steps

After refactor, verify:
1. mvn clean package -DskipTests -- must pass with no compilation errors
2. docker compose down && docker compose up --build
3. App starts cleanly -- check logs for no bean creation errors
4. Flyway reports no migration changes needed
5. Swagger loads and all endpoints visible
6. POST a new entity via Swagger -- verify createdAt and updatedAt are populated
7. PUT an existing entity via Swagger -- verify updatedAt changes, createdAt stays same

---

## Acceptance Criteria
- [ ] All 9 entities extend BaseEntity
- [ ] No entity has its own createdAt or updatedAt field declarations
- [ ] No entity has @EntityListeners annotation
- [ ] No entity imports CreatedDate, LastModifiedDate, AuditingEntityListener
- [ ] mvn clean package -DskipTests passes
- [ ] App starts cleanly
- [ ] createdAt populated on insert via Swagger test
- [ ] updatedAt populated on insert and changes on update via Swagger test
- [ ] No schema changes or migration errors
