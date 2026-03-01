# SmartFinances — Docker Master Specification

## Overview
This document defines the Docker standards and conventions for the SmartFinances project. All Docker-related work must follow these guidelines. This spec is referenced by all feature specs that involve containerization, deployment, or infrastructure changes.

---

## Base Image Standard

All SmartFinances service images must use:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
```

- `eclipse-temurin` — official OpenJDK distribution, actively maintained
- `21` — matches project Java version, must stay in sync with `pom.xml`
- `jre` — runtime only, never JDK in production images
- `alpine` — minimal Linux base, keeps image size small

Never use `latest` tag. Always pin to a specific Java version.

---

## Dockerfile Standards

### Spring Boot Application
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/smartfinances-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Rules
- Always set `WORKDIR` — never copy files into root
- Rename the jar to a version-agnostic name (`app.jar`) so the Dockerfile never needs updating on version bumps
- `EXPOSE` is informational only — actual port mapping happens at runtime
- Use `ENTRYPOINT` not `CMD` for the main process
- The jar must be built before running `docker build` — Docker does not run Maven

### Build Command
```bash
mvn clean package -DskipTests
docker build -t smartfinances .
```

---

## Environment Variables

### Principles
- **Never hardcode** credentials, secrets, or environment-specific values in the Dockerfile or docker-compose.yml
- All configurable values must be injected via environment variables at runtime
- Application properties use `${VAR_NAME:default}` syntax — default values are for local development only, never for production
- Production secrets are managed via AWS Secrets Manager, never in code or version control

### Naming Convention
All environment variables use `UPPER_SNAKE_CASE`:

| Variable | Description | Default (local only) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `DB_URL` | Full JDBC connection URL | `jdbc:postgresql://localhost:5432/smartfinances` |
| `DB_USERNAME` | Database username | `lucky` |
| `DB_PASSWORD` | Database password | _(empty)_ |
| `JWT_SECRET` | JWT signing secret | `local-dev-secret-change-in-production` |
| `JWT_EXPIRATION` | Access token expiry in seconds | `900` |
| `JWT_REFRESH_EXPIRATION` | Refresh token expiry in seconds | `604800` |

### Adding New Variables
When a new configurable value is introduced:
1. Add it to this table
2. Add the `${VAR_NAME:default}` reference in the appropriate `application-*.properties`
3. Add it to `docker-compose.yml` environment section
4. Document it in the AWS deployment spec when applicable

---

## Docker Compose Standards

### Purpose
Docker Compose is for **local development only**. It is never used in production. ECS replaces Compose in the AWS environment.

### Service Naming Convention
Services are named after their role in lowercase:

| Service Name | Description |
|---|---|
| `app` | Spring Boot application |
| `db` | PostgreSQL database (future) |
| `ui` | React frontend (future) |
| `mcp` | MCP server (future) |

### Current docker-compose.yml
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

### Rules
- Always use `host.docker.internal` to reach services running on the host machine (local PostgreSQL)
- When PostgreSQL is added as a Compose service, use `depends_on` with `condition: service_healthy`
- Never commit real secrets to `docker-compose.yml` — use `.env` file for local overrides
- Port mapping format is always `host:container`

### .env File (local overrides)
For sensitive local values create a `.env` file in the project root — this file is gitignored:

```
DB_PASSWORD=yourlocalpassword
JWT_SECRET=your-local-secret
```

Docker Compose automatically reads `.env` — no extra configuration needed.

---

## Port Conventions

| Service | Internal Port | External Port (local) |
|---|---|---|
| Spring Boot app | 8080 | 8080 |
| PostgreSQL | 5432 | 5432 |
| React UI (future) | 3000 | 3000 |

Internal ports never change. External ports may be adjusted locally if there are conflicts.

---

## Health Check Standards

When a service is added as a Compose dependency, it must include a health check:

```yaml
db:
  image: postgres:14-alpine
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
    interval: 5s
    timeout: 5s
    retries: 5
```

Dependent services use:
```yaml
depends_on:
  db:
    condition: service_healthy
```

---

## .dockerignore

A `.dockerignore` file must exist in the project root to keep images lean:

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

## Adding a New Service

When a new service is added to the project (UI, MCP server etc):
1. Create a `Dockerfile` in that service's directory
2. Add the service to `docker-compose.yml` following the naming convention
3. Add any new environment variables to this spec
4. Update port conventions table if applicable
5. Add health check if other services depend on it

---

## What Docker Is NOT Responsible For

- **Database schema** — managed by Flyway, runs on app startup
- **Secrets rotation** — managed by AWS Secrets Manager in production
- **Scaling** — managed by ECS in production
- **SSL/TLS** — managed by the AWS Load Balancer in production
