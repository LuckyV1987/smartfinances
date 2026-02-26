# SmartFinances — Folder Structure Setup
*Throwaway spec — give this to Copilot to create the project skeleton*

---

## Task
Create the following package and folder structure for a Spring Boot 3 project.
Base package: `com.smartfinances`
Empty shells only — correct class-level annotations, no method implementations.

---

## Backend Package Structure

```
src/main/java/com/smartfinances/
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
│   ├── request/
│   └── response/
├── exception/
└── mapper/
```

---

## Resources Structure

```
src/main/resources/
├── application.properties
├── application-dev.properties
├── application-prod.properties
└── db/
    └── migration/
```

---

## Docs Structure

```
docs/
├── architecture/
│   ├── overview.md
│   ├── data-model.md
│   └── decisions/
├── specs/
│   ├── master/
│   └── features/
│       ├── completed/
│       └── in-progress/
├── startup/
└── context.md
```

---

## application.properties baseline

```properties
# Application
spring.application.name=smartfinances

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/smartfinances
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Actuator
management.endpoints.web.exposure.include=health,info
```

---

## Notes for Copilot
- Create packages and empty placeholder files only
- No entity classes, no service classes yet — folders only
- Place application.properties content as shown above
- Create empty application-dev.properties and application-prod.properties
- Create empty overview.md, data-model.md and context.md
