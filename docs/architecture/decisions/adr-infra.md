# ADR-019: Deployment Strategy — Elastic Beanstalk First, ECS Later

**Status:** Accepted  
**Date:** 2026-03-01  
**Decision Makers:** Lucky (Architect)

## Context
SmartFinances needs a deployment strategy that balances cost, learning value, and resume impact. Options considered:

1. Elastic Beanstalk on EC2 (free tier eligible)
2. Docker + ECS Fargate (modern, paid)
3. Self-hosted (home server or Raspberry Pi)

## Decision
Deploy to **Elastic Beanstalk first**, migrate to **Docker + ECS Fargate** once free tier expires or the app is stable enough to justify the cost.

## Rationale
- Free tier covers EC2 t2.micro and RDS db.t3.micro for 12 months — zero cost to start
- Gets a live deployable URL quickly for portfolio and interviews
- Elastic Beanstalk handles infrastructure complexity automatically, allowing focus on application code
- Docker is already implemented locally — migration to ECS is a natural next step, not a rework
- ECS Fargate + Docker is the modern industry standard and significantly stronger on a resume

## Single Environment Decision
A separate dev environment on AWS is not justified at ~$43/month per environment for a solo portfolio project. The strategy is:

- Local machine → development
- Main branch → production (single AWS environment)

Disciplined local testing and CI/CD gating replace a separate dev environment.

## Future Upgrade Path
```
Phase 1 (now)     → Elastic Beanstalk + RDS (free tier)
Phase 2 (later)   → Docker + ECS Fargate + RDS + ALB (~$43/month)
Phase 3 (optional)→ Self-hosted via Coolify/Dokku with Cloudflare Tunnel
```

## Consequences
- Positive: Zero cost for 12 months, live URL available quickly
- Positive: CI/CD pipeline and RDS integration concepts are identical across phases
- Negative: Elastic Beanstalk is less impressive on a resume than ECS
- Mitigation: Docker is already containerized locally — ECS migration is straightforward when ready

---

# ADR-020: Spend Category — System vs User Defined

**Status:** Accepted  
**Date:** 2026-03-01  
**Decision Makers:** Lucky (Architect)

## Context
SmartFinances needs spend categories for classifying transactions and enabling analytics. Options considered:

1. Single table with `is_system` flag to distinguish system and user categories
2. Separate tables for system and user categories with a FK relationship
3. Single user-defined table with no system defaults

## Decision
**Separate tables** — `spend_categories` for system defaults and `user_spend_categories` for user-defined categories, with `parent_spend_category_id` FK on the user table pointing to the system table.

## Rationale
- System categories are staff-managed reference data — they have different ownership, lifecycle, and access patterns than user categories
- Keeping them separate avoids filtering complexity (`WHERE is_system = true`) throughout every query
- The FK on `user_spend_categories` provides the rollup mapping needed for analytics without a bridge table — a user category maps to exactly one system category (one-to-many, not many-to-many)
- Analytics always roll up to system categories regardless of what users name their custom categories

## Analytics Benefit
```
system category: Dining
    ↑ parent_spend_category_id
user category: Date Night      → transaction
user category: Work Lunch      → transaction
user category: Fast Food       → transaction

Query: "Total dining spend this month" = sum across all three user categories
```

## Access Pattern
- `spend_categories` — staff managed via admin controller, Swagger only, no user access
- `user_spend_categories` — user managed via standard API, requires authentication

## Consequences
- Positive: Clean separation of concerns, no flag-based filtering
- Positive: Analytics rollup is a simple FK join
- Positive: Each table evolves independently without affecting the other
- Negative: Two tables to maintain instead of one
- Acceptable: The complexity is justified by the analytics and access pattern benefits

---

# ADR-021: Spend Category Type Enum — INFLOW vs INCOME

**Status:** Accepted  
**Date:** 2026-03-01  
**Decision Makers:** Lucky (Architect)

## Context
Spend categories need a type to distinguish money coming in from money going out. Options considered:

1. `INCOME` / `EXPENSE`
2. `INFLOW` / `EXPENSE`
3. `CREDIT` / `DEBIT`

## Decision
Use **`INFLOW` / `EXPENSE`** as the category type enum.

## Rationale
- `INCOME` implies employment or earned income specifically — excludes gifts, investment returns, reimbursements
- `INFLOW` is neutral and covers any money entering the system regardless of source
- `EXPENSE` is universally understood and accurate
- `CREDIT` / `DEBIT` are accounting terms better suited to transaction direction, not category classification — using them for both would create naming confusion

## Consequences
- Positive: More accurate domain language for a personal finance context
- Positive: No ambiguity when categorizing non-income inflows like gifts or investment returns
- Neutral: Minor deviation from common terminology — easily explained in documentation

---

# ADR-022: Spring Boot Version — 3.4.3 over 4.0.3

**Status:** Accepted  
**Date:** 2026-03-01  
**Decision Makers:** Lucky (Architect)

## Context
SmartFinances was initially built on Spring Boot 4.0.3. During Swagger integration, springdoc-openapi was found to be incompatible with Spring Boot 4.x due to breaking changes in Spring 7's internal APIs (`ControllerAdviceBean` constructor signature change). Options considered:

1. Stay on Spring Boot 4.0.3 and wait for springdoc to release Spring Boot 4 support
2. Downgrade to Spring Boot 3.4.3 (latest stable 3.x) for full ecosystem compatibility
3. Remove Swagger entirely and use a static OpenAPI spec

## Decision
Downgrade to **Spring Boot 3.4.3**.

## Rationale
- Spring Boot 4.0.3 is very new — the broader ecosystem (springdoc, various libraries) has not caught up
- springdoc is essential for API documentation and Swagger UI, which is a portfolio and staff tooling requirement
- Spring Boot 3.4.3 is the current stable LTS-aligned release with full ecosystem support
- The Java version (21) and all other dependencies remain unchanged — the downgrade is low risk
- Waiting for springdoc Spring Boot 4 support is an indefinite timeline with no clear ETA

## Investigation Performed
- Confirmed springdoc 2.8.4 autoconfiguration loads correctly on 3.4.3
- Confirmed the `ControllerAdviceBean` incompatibility is a Spring 7 breaking change, not a configuration issue
- Confirmed no other dependencies were affected by the downgrade

## Consequences
- Positive: Full ecosystem compatibility, Swagger UI working
- Positive: 3.4.3 is a well-supported, stable release suitable for production
- Negative: Not on the latest Spring Boot version
- Future: Revisit upgrade to Spring Boot 4.x when springdoc releases official support
