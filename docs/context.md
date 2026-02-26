# SmartFinances — Project Context
*Paste this at the start of every agent session to onboard the agent instantly.*
*Update this file every time a feature is completed.*

---

## What is this?
A household budgeting app built spec-first using AI-assisted development.
The Engineering Manager writes specs and reviews output. The agent implements.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4.0.3, Java 21 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Frontend | React |
| Cloud | AWS |
| Build | Maven |
| AI Tool | GitHub Copilot (Claude Sonnet 4.6) |

---

## Package Structure
```
com.smartfinances
  .config
  .controller
  .service
  .repository
  .entity
  .dto.request
  .dto.response
  .exception
  .mapper
```

---

## Standards
Always follow `docs/specs/master/engineering-standards.md`:
- Constructor injection only
- DTOs never below service layer
- Entities never above service layer
- No business logic in controllers
- All exceptions thrown from service, caught by GlobalExceptionHandler
- All entities extend BaseEntity
- Soft delete via `deleted_at` on transactional entities

---

## What is Built
*Update this section as features are completed*

- [ ] Project skeleton and folder structure
- [ ] BaseEntity
- [ ] Flyway baseline migration
- [ ] Core entities

---

## What is In Progress
*Update this section with the current active task*

- None yet

---

## Known Decisions
See `docs/architecture/decisions/ADR.md` for full list.

Key decisions:
- Layered architecture — Controller → Service → Repository → Database
- Spring auditing for all audit fields — no database triggers
- Selective soft delete — `deleted_at` on transactional entities only
- Spec-driven development — no code without a spec
