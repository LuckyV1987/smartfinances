# REFACTOR — Ownership Entity Controller: Service Split & Docker Build Fix

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/controller-spec.md`
- `docs/specs/master/service-spec.md`

Reference these existing files:
- `src/main/java/com/smartfinances/controller/OwnershipEntityController.java`
- `src/main/java/com/smartfinances/service/OwnershipEntityService.java`

---

## Type
Refactor — no new features, no schema changes, no migrations

## Note
No branch required — changes not yet pushed. Apply directly to current working state.

Cut from: `develop`

---

## Background & Decisions

### Decision 1 — Two Services, One Controller

During implementation membership logic was bundled into OwnershipEntityService.
This violates SRP.

**Resolution:** Split into two services:
- `OwnershipEntityService` — entity lifecycle only
- `OwnershipMembershipService` — membership lifecycle only

**Controller stays as ONE class.** Membership endpoints are sub-resources of
ownership entities (e.g. /api/ownership-entities/{id}/members) — always accessed
in the context of an entity. Splitting the controller would produce unnatural URLs
like /api/memberships which loses the resource hierarchy.

SRP at the service layer is what matters for business logic separation. At the
controller layer, grouping by resource hierarchy is the correct REST pattern.
The controller depends on both services via constructor injection.

### Decision 2 — Docker Build Was Using Stale Jar

New controllers were not appearing in Swagger because Docker was running a cached
image with the old jar. docker compose up without --build does not recompile.

Resolution: Always use docker compose up --build after any code change.

Also confirmed: Maven was resolving to JDK 24 (Homebrew) instead of JDK 21 (Oracle)
due to PATH ordering. Fixed by adding export PATH=$JAVA_HOME/bin:$PATH after the
JAVA_HOME declaration in ~/.zshrc.

---

## Scope of Changes

### 1. Create OwnershipMembershipService
File: src/main/java/com/smartfinances/service/OwnershipMembershipService.java

Annotate with @Service. Constructor injection only.
Dependencies: OwnershipMembershipRepository, OwnershipEntityRepository,
UserRepository, OwnershipEntityMapper

Methods (moved from OwnershipEntityService):

findMembersByEntity(Long ownershipEntityId) -> List<MembershipResponseDTO>
- Returns all active memberships for the given entity
- Throws ResourceNotFoundException if entity not found or inactive

addMember(Long ownershipEntityId, MembershipRequestDTO dto) -> MembershipResponseDTO
- Find entity -> ResourceNotFoundException if not found or inactive
- Find user -> ResourceNotFoundException if not found
- Check active membership exists -> DuplicateResourceException if so
- Save and return mapped DTO

removeMember(Long ownershipEntityId, Long userId) -> void
- Find entity -> ResourceNotFoundException if not found or inactive
- Find active membership -> ResourceNotFoundException if not found
- Set active = false and save (soft delete)

createMembership(OwnershipEntity entity, User user) -> OwnershipMembership
- Internal only — not exposed via controller
- Called by UserService/AuthService during signup
- Check active membership exists -> DuplicateResourceException if so
- Save and return

### 2. Update OwnershipEntityService
File: src/main/java/com/smartfinances/service/OwnershipEntityService.java

- Remove all membership methods (moved to OwnershipMembershipService)
- Remove OwnershipMembershipRepository and UserRepository from dependencies
- Dependencies become: OwnershipEntityRepository, OwnershipEntityMapper only

Methods that remain:
findAll, findById, findByType, findEntitiesByUser,
createPersonalOwnershipEntity, create, update, deactivate

### 3. Update OwnershipEntityController
File: src/main/java/com/smartfinances/controller/OwnershipEntityController.java

- Add OwnershipMembershipService as second constructor-injected dependency
- getMembersByEntity() -> calls ownershipMembershipService.findMembersByEntity()
- addMember() -> calls ownershipMembershipService.addMember()
- removeMember() -> calls ownershipMembershipService.removeMember()
- All entity endpoints remain on ownershipEntityService
- No URL changes

---

## What Does NOT Change
- No endpoint URLs
- No DTOs
- No database schema or migrations
- No security config
- Swagger output is identical

---

## Build Reminder
After this refactor:
  mvn clean package -DskipTests
  docker compose down
  docker compose up --build

Never use docker compose up without --build after code changes.

---

## Tests

Unit — update OwnershipEntityServiceTest.java
- Remove membership test cases

New — OwnershipMembershipServiceTest.java
- shouldReturnMembers_whenFindByEntity
- shouldThrowException_whenEntityNotFoundOnFindMembers
- shouldAddMember_whenValidRequest
- shouldThrowException_whenMemberAlreadyExists
- shouldRemoveMember_whenValidRequest
- shouldThrowException_whenMembershipNotFound
- shouldCreateMembership_whenCalledInternally

Integration — OwnershipEntityControllerIntegrationTest.java
- No changes needed, endpoints and behaviour identical

---

## Acceptance Criteria
- [ ] OwnershipEntityService has no membership methods or dependencies
- [ ] OwnershipMembershipService is a separate @Service bean
- [ ] OwnershipEntityController depends on both services via constructor injection
- [ ] All endpoint URLs unchanged
- [ ] All existing integration tests pass
- [ ] New OwnershipMembershipService unit tests pass
- [ ] App starts cleanly with no bean creation errors
- [ ] All endpoints visible in Swagger after docker compose up --build
