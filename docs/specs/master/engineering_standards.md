# SmartFinances — Engineering Standards & Conventions
*Read this before writing any code. These standards apply to every feature.*

---

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Java classes | PascalCase | `TransactionService`, `BudgetCategory` |
| Java methods | camelCase | `findByHouseholdId()` |
| Java fields | camelCase | `createdAt`, `householdId` |
| Database tables | snake_case plural | `budget_categories`, `transaction_items` |
| Database columns | snake_case | `created_at`, `household_id` |
| Packages | lowercase | `com.smartfinances.service` |
| Constants | UPPER_SNAKE_CASE | `MAX_BUDGET_LIMIT` |

---

## Project Structure

```
smartfinances/
├── docs/
│   ├── architecture/
│   │   ├── decisions/          # ADR files
│   │   ├── data-model.md       # Entity relationships and diagram
│   │   └── overview.md         # System overview
│   ├── specs/
│   │   ├── master/             # Standards — always reference in agent sessions
│   │   └── features/
│   │       ├── completed/      # Done — audit trail
│   │       └── in-progress/    # Active tasks
│   ├── startup/                # Setup instructions and throwaway docs
│   └── context.md              # Living doc — paste to every agent session
├── backend/                    # Spring Boot application
├── frontend/                   # React application
└── README.md                   # Showcase document
```

---

## Entity Standards

Every entity must extend BaseEntity:

```java
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

BaseEntity definition:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private Long createdBy;

    @LastModifiedBy
    private Long updatedBy;
}
```

Lombok on entities:
- Use `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- Never use `@Data` on entities
- Write `equals()` and `hashCode()` manually based on `id` only

Relationships:
- Foreign key always on the `@ManyToOne` side
- Always specify `@JoinColumn(name = "foreign_key_column")`
- Default fetch type — LAZY on all collections
- Use `mappedBy` on the `@OneToMany` side

Soft delete:
- Soft-deletable entities carry a `deleted_at`