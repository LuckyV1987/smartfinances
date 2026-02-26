# SmartFinances — Data Model

## Entity Relationships

```
Household
  ├── id (PK)
  ├── name
  └── [audit fields]
       │
       ├──< Member
       │      ├── id (PK)
       │      ├── household_id (FK)
       │      ├── user_id (FK)
       │      └── [audit fields]
       │
       ├──< Account
       │      ├── id (PK)
       │      ├── household_id (FK)
       │      ├── name
       │      ├── type
       │      ├── balance
       │      └── [audit fields]
       │
       ├──< BudgetCategory
       │      ├── id (PK)
       │      ├── household_id (FK)
       │      ├── name
       │      ├── allocated_amount
       │      ├── deleted_at
       │      └── [audit fields]
       │
       └──< Transaction
              ├── id (PK)
              ├── household_id (FK)
              ├── member_id (FK)
              ├── account_id (FK)
              ├── budget_category_id (FK)
              ├── amount
              ├── description
              ├── date
              ├── deleted_at
              └── [audit fields]
                   │
                   └──< TransactionItem
                          ├── id (PK)
                          ├── transaction_id (FK)
                          ├── description
                          ├── amount
                          └── [audit fields]
```

---

## Relationship Summary

| Relationship | Type | Foreign Key |
|-------------|------|-------------|
| Household → Member | One to Many | member.household_id |
| Household → Account | One to Many | account.household_id |
| Household → BudgetCategory | One to Many | budget_category.household_id |
| Household → Transaction | One to Many | transaction.household_id |
| Member → Transaction | One to Many | transaction.member_id |
| Account → Transaction | One to Many | transaction.account_id |
| BudgetCategory → Transaction | One to Many | transaction.budget_category_id |
| Transaction → TransactionItem | One to Many | transaction_item.transaction_id |

---

## Soft Delete Entities

| Entity | Reason |
|--------|--------|
| Transaction | User correction, audit trail |
| BudgetCategory | Historical analytics integrity |

All other entities use hard delete.

---

## Audit Fields
All entities inherit the following from BaseEntity:

| Field | Type | Behaviour |
|-------|------|-----------|
| created_at | TIMESTAMP | Set on insert, never updated |
| updated_at | TIMESTAMP | Updated on every save |
| created_by | BIGINT | Set on insert, never updated |
| updated_by | BIGINT | Updated on every save |

---

## Notes
- All primary keys are BIGSERIAL (auto-incrementing Long)
- All foreign keys have corresponding indexes
- Data model is subject to change as features are added
- See `docs/architecture/decisions/ADR.md` for decisions that shaped this model
