# Architecture Decision Records (ADRs)
*SmartFinances — Engineering decisions and the reasoning behind them*

---

## ADR-001: Layered Architecture
**Decision**: Adopt a strict layered architecture — Controller → Service → Repository → Database

**Reasoning**: Each layer has a single responsibility. Business logic lives exclusively in the service layer, making it reusable, testable, and independent of HTTP or database concerns. Controllers stay thin. Repositories stay dumb.

**Impact**: DTOs never cross below the service layer. Entities never cross above it. The service is the sole translator between the two worlds.

---

## ADR-002: Constructor Injection Over Field Injection
**Decision**: All Spring dependencies injected via constructor, never via @Autowired on fields.

**Reasoning**: Constructor injection makes dependencies explicit, enables immutability via `final` fields, and allows unit testing without spinning up a Spring context. Field injection hides dependencies and produces untestable code.

**Impact**: All Spring beans use constructor injection. Dependencies are immediately visible from the class signature.

---

## ADR-003: Global Exception Handling via @ControllerAdvice
**Decision**: All exception handling centralised in a single GlobalExceptionHandler class.

**Reasoning**: Scattering try/catch blocks across controllers violates DRY and produces inconsistent error responses. A single handler ensures every error returns a clean, consistent JSON response with the correct HTTP status code.

**Impact**: Controllers contain zero try/catch blocks. Custom exceptions are thrown from the service layer and caught globally.

---

## ADR-004: DTO Separation at Service Boundary
**Decision**: Separate Request DTOs, Response DTOs, and Entities. Mapping happens in the service layer.

**Reasoning**: Exposing entities directly from REST endpoints leaks internal data model details, creates security risks, and tightly couples the API contract to the database schema. DTOs decouple these concerns.

**Impact**: Every endpoint accepts a RequestDTO and returns a ResponseDTO. Entities are internal implementation details invisible to API consumers.

---

## ADR-005: Spring Data JPA Auditing for Audit Fields
**Decision**: Use Spring Data JPA auditing (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy) rather than database triggers or manual field assignment.

**Reasoning**: Database triggers add invisible complexity and are hard to test. Manual field assignment is error-prone and scattered. Spring auditing centralises this concern in a single BaseEntity, is fully testable, and stays within the application layer where it is visible and maintainable.

**Impact**: All entities extend BaseEntity. Audit fields are populated automatically. No database triggers exist in the schema.

---

## ADR-006: Selective Lombok Usage
**Decision**: Use Lombok selectively — @Getter @Setter on entities, @Data on DTOs, @Builder where construction complexity warrants it.

**Reasoning**: Lombok generates code that is invisible in the source file. When something breaks — a getter behaves unexpectedly, a build fails — there is nothing to read. On DTOs this is acceptable since they are simple data carriers. On entities it is risky because JPA interacts with them at a deep level — @Data generates equals/hashCode based on all fields which causes problems with lazy loading and circular references. Explicit annotations on entities keep behaviour visible and safe.

**Impact**: Entities use explicit @Getter @Setter. DTOs use @Data. equals/hashCode on entities are written manually based on id only.

---

## ADR-007: Spec-Driven AI-Assisted Development
**Decision**: All features begin with a written spec before any code is generated. Master specs define standards. Feature specs define specifics. All specs live in the repository.

**Reasoning**: AI agents