# SmartFinances

A household budgeting application built spec-first using AI-assisted development. Families can manage budget categories, track transactions, and gain deep analytics into spending patterns and predictions.

> **This project is actively in development. Follow the journey in the commit history.**

---

## Tech Stack

![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?style=flat&logo=spring-boot)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react)
![AWS](https://img.shields.io/badge/AWS-Elastic_Beanstalk_|_RDS_|_S3-FF9900?style=flat&logo=amazon-aws)
![GitHub Copilot](https://img.shields.io/badge/GitHub_Copilot-Claude_Sonnet_4.6-000000?style=flat&logo=github)

---

## Features

- Household and member management
- Budget category allocation
- Transaction tracking with receipt scanning via AI
- Deep analytics — spending patterns, trends, predictions
- Multi-member expense splitting

---

## Architecture

```
React Frontend
      │
Spring Boot REST API
      │
PostgreSQL (AWS RDS)
```

Strict layered architecture — Controller → Service → Repository → Database.
Full architecture documentation in `docs/architecture/`.

---

## AI-Assisted Development

This project is built using a spec-driven AI-assisted development workflow:

- Every feature starts as a written spec before any code is generated
- GitHub Copilot (Claude Sonnet 4.6) implements specs
- Engineering Manager reviews, refactors, and approves all output
- Architecture decisions are documented as ADRs in `docs/architecture/decisions/`

This workflow demonstrates how a modern engineering leader can leverage AI tooling to ship production quality software rapidly without sacrificing architecture or code quality.

---

## Project Structure

```
smartfinances/
├── docs/
│   ├── architecture/        # System overview, data model, ADRs
│   ├── specs/               # Master standards and feature specs
│   ├── startup/             # Setup instructions
│   └── context.md           # Living project context document
├── backend/                 # Spring Boot application
├── frontend/                # React application
└── README.md
```

---

## Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL 16
- Node.js 20

### Backend Setup
```bash
cd backend
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
# Add your database credentials
mvn spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm start
```

---

## Documentation

| Document | Description |
|----------|-------------|
| `docs/architecture/overview.md` | System architecture and request flow |
| `docs/architecture/data-model.md` | Entity relationships |
| `docs/architecture/decisions/ADR.md` | All architecture decisions and reasoning |
| `docs/specs/master/engineering-standards.md` | Coding standards and conventions |

---

## Status

| Layer | Status |
|-------|--------|
| Project setup | 🔲 In progress |
| Database migrations | 🔲 Not started |
| Backend APIs | 🔲 Not started |
| Frontend | 🔲 Not started |
| AWS Deployment | 🔲 Not started |
| Analytics | 🔲 Not started |
| AI Receipt Scanning | 🔲 Not started |

---

## Author
Lucky — Senior Software Engineer & Engineering Manager
13 years enterprise Java | High-throughput systems | AI-assisted development
