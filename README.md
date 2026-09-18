# Employee Management System

A full-stack Employee Management System with JWT authentication, role-based access control, and a complete task approval workflow — built with Spring Boot and React.

> 🚧 **Status:** Backend and frontend are functionally complete and fully tested. AWS deployment is in progress — a live demo link will be added here once available.

## Features

- **Authentication & Authorization** — JWT-based login with three roles (Admin, Manager, Employee), enforced at both the API and UI layers
- **User management** — full CRUD, role changes, department assignment, self-service and admin-initiated password resets
- **Department management** — CRUD, single-manager-per-department constraint, safeguards against deleting departments that still have employees
- **Task workflow** — a full approval cycle (Assigned → Started → Under Review → Done, or rejected back to Assigned), with role- and state-aware permissions at every step
- **Audit logging** — every significant action (user changes, role changes, task transitions) is recorded and viewable by admins
- **Dashboard** — role-scoped stats (task breakdown, overdue tasks, team size, org totals)

## Tech stack

**Backend**
- Java 21, Spring Boot 3
- Spring Security + JWT
- Spring Data JPA / Hibernate
- MySQL 8 (production/dev), H2 (tests)
- Springdoc OpenAPI (Swagger UI)

**Frontend**
- React + TypeScript, built with Vite
- Material UI
- React Router
- Axios

**Testing**
- JUnit 5 + Mockito (unit tests, service layer)
- Spring Boot Test + MockMvc + H2 (integration tests, proving role-based access control end to end over real HTTP requests)

**DevOps**
- Docker (multi-stage build, ~131MB runtime image)
- Docker Compose (backend + MySQL, healthcheck-gated startup)
- GitHub Actions CI (runs the full test suite on every push)

## Architecture

```
├── src/                    Spring Boot backend
│   ├── controller/          REST endpoints
│   ├── service/              Business logic + authorization rules
│   ├── repository/         Spring Data JPA repositories
│   ├── model/               JPA entities
│   ├── dto/                  Request/response DTOs
│   ├── security/           JWT filter, security config
│   └── exception/           Global exception handling
├── frontend/                React + TypeScript frontend
│   └── src/
│       ├── pages/            Route-level pages
│       ├── components/    Reusable UI (dialogs, layout)
│       ├── api/              Axios calls per resource
│       ├── context/         Auth state
│       └── types/            TypeScript types matching backend DTOs
├── Dockerfile               Multi-stage backend build
├── docker-compose.yml       Backend + MySQL, networked together
└── .github/workflows/       CI pipeline
```

## Getting started

### Prerequisites
- Docker Desktop
- Node.js 18+ and npm

### Backend + database

```bash
# from the repo root
cp .env.example .env        # fill in real values
docker compose up --build
```

The API will be available at `http://localhost:8080`, with interactive docs at `http://localhost:8080/swagger-ui/index.html`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

> **Note:** you'll need at least one user in the database to log in. See the backend tests or Swagger UI to create an initial admin user directly against a fresh database.

## Running the tests

```bash
mvn test
```

Runs the full suite — unit tests (mocked dependencies, no database) and integration tests (real Spring context + H2 in-memory database), including tests that verify unauthorized roles are actually rejected over real HTTP requests, not just in business logic.

## Roles at a glance

| Role | Can do |
|---|---|
| **Admin** | Full access — manage users, departments, roles, and view the audit log |
| **Manager** | Manage tasks and employees within their own department; assign/review tasks for their team |
| **Employee** | View and work their own assigned tasks |

## Author

**Osamah Al-Obaidi**
[GitHub](https://github.com/Alobaidi95) · [LinkedIn](https://www.linkedin.com/in/osamah-al-obaidi95/)
