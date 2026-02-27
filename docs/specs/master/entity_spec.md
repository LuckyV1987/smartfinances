# SmartFinances — Master Entity Spec
*Reference this in every agent session that involves creating or modifying entities.*

---

## Rules

- Every entity extends `BaseEntity`
- Every entity lives in `com.smartfinances.entity`
- Every entity maps to a snake_case plural table name via `@Table(name = "...")`
- Primary key is always `Long id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- Never use `@Data` on entities
- Write `equals()` and `hashCode()` manually based on `id` only
- All relationships default to `FetchType.LAZY`
- Foreign key always on the `@ManyToOne` side via `@JoinColumn`
- Use `mappedBy` on the `@OneToMany` side

---

## Lombok on Entities

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
```

Never `@Data`. Never `@EqualsAndHashCode`. Always write equals/hashCode manually.

---

## BaseEntity

Every entity extends this class. Never modify BaseEntity without an ADR.

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

---

## Soft Delete

Entities that require soft delete must:

1. Carry a `deleted_at` column:
```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

2. Be annotated with:
```java
@Where(clause = "deleted_at IS NULL")
```

3. Never call `repo.delete()` — always set `deletedAt` and save:
```java
entity.setDeletedAt(LocalDateTime.now());
repo.save(entity);
```

Soft delete entities: `Transaction`, `BudgetCategory`
All others: hard delete

---

## Standard Entity Template

```java
@Entity
@Table(name = "table_name_plural")
@Where(clause = "deleted_at IS NULL")  // only on soft delete entities
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityName extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fields here

    // @ManyToOne relationships here
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "foreign_key_id")
    private RelatedEntity relatedEntity;

    // @OneToMany relationships here
    @OneToMany(mappedBy = "thisEntity", fetch = FetchType.LAZY)
    private List<ChildEntity> children = new ArrayList<>();

    // equals and hashCode based on id only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityName)) return false;
        EntityName that = (EntityName) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

## Relationships Summary

| Annotation | Side | Foreign Key |
|------------|------|-------------|
| `@ManyToOne` | Many side | Lives here — `@JoinColumn` |
| `@OneToMany` | One side | `mappedBy` references field on many side |
| `@OneToOne` | Owner side | `@JoinColumn` |
| `@ManyToMany` | Owner side | `@JoinTable` |

---

## What NOT to do

- Never put `@Data` on an entity
- Never use `FetchType.EAGER` without an ADR
- Never return an entity directly from a controller
- Never put business logic in an entity
- Never skip `@Table(name = "...")` — always be explicit
