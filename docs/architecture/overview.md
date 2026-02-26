# SmartFinances — Architecture Overview

## What is SmartFinances?
A household budgeting application that allows families to manage budget categories, track transactions, and gain deep analytics into spending patterns and predictions. Built spec-first using AI-assisted development with GitHub Copilot.

---

## System Architecture

```
┌─────────────────────────────────────────────────────┐
│                    React Frontend                    │
│              (Budget, Transactions, Analytics)       │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP / REST
┌─────────────────────▼───────────────────────────────┐
│                 Spring Boot Backend                  │
│                                                      │
│  Controller → Service → Repository                   │
│                                                      │
│  - REST API                                          │
│  - Business Logic                                    │
│  - Spring Security (JWT)                             │
│  - Spring Data JPA                                   │
│  - Flyway Migrations                                 │
└─────────────────────┬───────────────────────────────┘
                      │ JDBC
┌─────────────────────▼───────────────────────────────┐
│                   PostgreSQL                         │
│              (AWS RDS in production)                 │
└─────────────────────────────────────────────────────┘
```

---

## Request Flow

```
HTTP Request
    → Controller        (validates input via @Valid)
    → Service           (business logic, DTO → Entity mapping)
    → Repository        (data access via Spring Data JPA)
    → Database          (PostgreSQL)
    ← Repository        (returns Entity)
    ← Service           (Entity → ResponseDTO mapping)
    ← Controller        (returns ResponseDTO as JSON)
HTTP Response
```

---

## Key Architectural Principles

- **Layered architecture** — strict separation of concerns across controller, service, and repository layers
- **DTO separation** — request and response DTOs decouple the API contract from the internal data model
- **Spec-driven development** — every feature starts as a written spec before any code is generated
- **AI-assisted implementation** — GitHub Copilot executes specs, Engineering Manager reviews and approves

---

## Infrastructure

| Component | Local | Production |
|-----------|-------|-----------|
| Backend | IntelliJ / localhost | AWS Elastic Beanstalk |
| Database | PostgreSQL local | AWS RDS PostgreSQL |
| File storage | Local | AWS S3 |
| CI/CD | — | GitHub Actions |
| Frontend | localhost:3000 | AWS S3 + CloudFront |

---

## Core Domain

```
Household
  ├── Members
  ├── Accounts
  ├── Budget Categories
  └── Transactions
        └── Transaction Items
```

---

## Security
- JWT-based authentication
- Role-based access — Household Admin, Member
- All endpoints secured except /auth/**
- Passwords hashed via BCrypt

---

## Further Reading
- `docs/architecture/decisions/ADR.md` — why each decision was made
- `docs/architecture/data-model.md` — entity relationships in detail
- `docs/specs/master/engineering-standards.md` — coding standards and conventions
