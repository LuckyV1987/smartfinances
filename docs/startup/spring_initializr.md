# SmartFinances — Spring Initializr Setup

## URL
https://start.spring.io

---

## Settings

| Field | Value |
|-------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.0.3 |
| Group | com.smartfinances |
| Artifact | smartfinances |
| Name | smartfinances |
| Packaging | Jar |
| Java | 21 |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Web | REST API support |
| Spring Data JPA | Database ORM and repository layer |
| PostgreSQL Driver | PostgreSQL database connectivity |
| Validation | @Valid and constraint annotations on DTOs |
| Lombok | Boilerplate code generation |
| Spring Security | Authentication and authorization |
| Flyway Migration | Database schema versioning and migrations |
| Spring Boot Actuator | Health checks and monitoring endpoints |

---

## Steps

1. Go to https://start.spring.io
2. Configure all settings above
3. Add all dependencies
4. Click Generate
5. Unzip the downloaded file
6. Open in IntelliJ — File → Open → select the unzipped folder
7. Wait for Maven to resolve all dependencies
8. Verify no errors in the Maven tool window
9. Run `SmartFinancesApplication.java` — should start and fail gracefully (no database yet)

---

## After Generation

Delete the following auto-generated files — they will be replaced:
- `src/test/java/com/smartfinances/SmartFinancesApplicationTests.java`

Do not modify `SmartFinancesApplication.java` yet.

---

## Next Step
Follow `docs/startup/folder-structure-setup.md` to create the project skeleton.
