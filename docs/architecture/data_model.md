# SmartFinances — Data Model
*Updated iteratively as features are built. Reflects current state only.*

---

## Current Version: V2

---

## Entity Relationships

```
User
  ├── id (PK)
  ├── email
  ├── password
  ├── first_name
  ├── last_name
  ├── phone_number
  ├── date_of_birth
  ├── deleted_at
  └── [audit fields]
       │
       └──< RefreshToken
              ├── id (PK)
              ├── user_id (FK)
              ├── token
              ├── expires_at
              └── [audit fields]
```

---

## Relationship Summary

| Relationship | Type | Foreign Key |
|-------------|------|-------------|
| User → RefreshToken | One to Many | refresh_token.user_id |

---

## Soft Delete Entities

| Entity | Reason |
|--------|--------|
| User | Account deactivation without data loss |

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
- See `docs/architecture/decisions/ADR.md` for decisions that shaped this model
- See `docs/architecture/decisions/ADR-010-jwt-authentication.md` for auth decisions

---

## Changelog
| Version | Change |
|---------|--------|
| V1 | Added users table |
| V2 | Added refresh_tokens table |
