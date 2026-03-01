# FEATURE — Docker Setup

## Agent Instructions
Before implementing read and follow:
- `docs/specs/master/engineering-standards.md`
- `docs/specs/master/branching-strategy.md`
- `docs/specs/master/docker-spec.md`

Reference these existing files for context:
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `pom.xml`

---

## Type
Infrastructure + Configuration

## Branch
Cut from: develop
Branch name: feature/docker-setup
Merge to: develop via PR
Commit message: feat: add Dockerfile and docker-compose for local development

---

## Scope
Dockerize the SmartFinances Spring Boot application for local development.
Adds a Dockerfile, docker-compose.yml, .dockerignore, and updates application
properties to use environment variables. PostgreSQL is not included as a Compose
service — the app connects to the existing local PostgreSQL instance via
`host.docker.internal`. Follow all conventions in `docker-spec.md`.

---

## 1. Update application.properties
**File:** `src/main/resources/application.properties`

```properties
spring.application.name=smartfinances
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

---

## 2. Update application-dev.properties
**File:** `src/main/resources/application-dev.properties`

```properties
# Database
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/smartfinances}
spring.datasource.username=${DB_USERNAME:lucky}
spring.datasource.password=${DB_PASSWORD:}

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT
jwt.secret=${JWT_SECRET:local-dev-secret-change-in-production}
jwt.expiration=${JWT_EXPIRATION:900}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800}

# Swagger
springdoc.swagger-ui.enabled=true
```

---

## 3. Dockerfile
**File:** `Dockerfile` (project root)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/smartfinances-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. docker-compose.yml
**File:** `docker-compose.yml` (project root)

```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:postgresql://host.docker.internal:5432/smartfinances
      DB_USERNAME: lucky
      DB_PASSWORD: ""
      JWT_SECRET: local-dev-secret-change-in-production
      JWT_EXPIRATION: 900
      JWT_REFRESH_EXPIRATION: 604800
```

---

## 5. .dockerignore
**File:** `.dockerignore` (project root)

```
.git
.gitignore
*.md
target/*.original
src/test
docker-compose.yml
.env
```

---

## 6. .env.example
**File:** `.env.example` (project root)

Document the required environment variables without real values.
This file is committed to version control as a reference.
The actual `.env` file is gitignored.

```
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://host.docker.internal:5432/smartfinances
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800
```

---

## 7. Update .gitignore
**File:** `.gitignore`

Ensure the following are present:
```
.env
*.original
```

---

## Verification Steps
After implementation, verify by running:

```bash
# Build the jar
mvn clean package -DskipTests

# Build the Docker image
docker build -t smartfinances .

# Run the container
docker compose up

# Verify app is running
curl http://localhost:8080/actuator/health

# Verify Swagger is accessible
curl http://localhost:8080/v3/api-docs
```

---

## Acceptance Criteria
- [ ] `docker build -t smartfinances .` completes successfully
- [ ] `docker compose up` starts the app container
- [ ] App connects to local PostgreSQL via `host.docker.internal`
- [ ] `http://localhost:8080/actuator/health` returns UP
- [ ] `http://localhost:8080/swagger-ui/index.html` is accessible
- [ ] No hardcoded credentials in any committed file
- [ ] `.env` is gitignored
- [ ] `.env.example` is committed as a reference
