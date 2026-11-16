# Pashe — agent context

This repository is the base for a full-stack healthcare support app called Pashe. The app is intentionally scaffolded but not yet feature-complete: the backend is a Spring Boot application, the frontend is a React + Vite app, and the database layer is PostgreSQL managed locally via Docker Compose.

## 1) Product direction

The current landing page describes Pashe as a care companion focused on:

- Guardian SOS / emergency support
- Ambulance / emergency transport
- Blood donation network
- Medicine care / reminders / requests

The app is meant to connect people with urgent care needs to trusted contacts, emergency services, and useful health support flows.

## 2) Project structure

```text
Pashe/
├── README.md
├── compose.yaml
├── AGENT_CONTEXT.md
├── backend/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── pashe/
│   │   │   │           └── app/
│   │   │   │               ├── BackendApplication.java
│   │   │   │               └── shared/
│   │   │   │                   └── health/
│   │   │   │                       └── HealthController.java
│   │   │   └── resources/
│   │   │       ├── application.yaml
│   │   │       └── db/
│   │   │           └── migration/
│   │   │               └── V1__initial_setup.sql
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── pashe/
│   │                   └── app/
│   │                       └── BackendApplicationTests.java
│   └── target/ (generated after build; optional)
├── frontend/
│   ├── README.md
│   ├── eslint.config.js
│   ├── index.html
│   ├── package.json
│   ├── tsconfig*.json
│   ├── vite.config.ts
│   ├── public/
│   └── src/
│       ├── App.css
│       ├── App.tsx
│       ├── index.css
│       ├── main.tsx
│       └── assets/
```

## 3) Current implementation status

### Backend

- Spring Boot application boots successfully.
- Application entry point: `backend/src/main/java/com/pashe/app/BackendApplication.java`
- Health endpoint exists at `/api/v1/health`
- Endpoint implementation: `HealthController` returns a simple JSON object:

```json
{
  "status": "ok",
  "application": "Pashe",
  "message": "Backend is running!"
}
```

- Database configuration is in `backend/src/main/resources/application.yaml`.
- Current Flyway migration is only a placeholder: `V1__initial_setup.sql` contains `SELECT 1;`.
- No real domain entities, repositories, services, controllers, or database schema exist yet.

### Frontend

- React app created with Vite.
- Main UI is a basic landing page in `frontend/src/App.tsx`.
- The page checks backend connectivity by calling `/api/v1/health`.
- There are no feature pages or reusable app architecture yet; this is a starter shell.

## 4) Stack and conventions

### Backend

- Java 21
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data JPA
- Flyway
- Validation
- PostgreSQL
- Maven wrapper (`backend/mvnw`)

### Frontend

- React 19
- TypeScript
- Vite
- ESLint

### Local infrastructure

- Postgres runs via Docker Compose from `compose.yaml`
- Database:
  - name: `pashe_db`
  - user: `pashe`
  - password: `pashe_local_dev`
  - port: `5432`

## 5) How to run the app locally

### Database

```bash
docker compose up -d postgres
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The app listens on:

- `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app typically runs on:

- `http://localhost:5173`

## 6) Example development workflow for a new feature

This repo is effectively a clean slate for the first real product features. The recommended approach is:

1. Define the domain model and API contract.
2. Add a Flyway migration for schema changes.
3. Implement backend entities, repositories, services, and controllers under `backend/src/main/java/com/pashe/app/...`.
4. Add frontend pages/components for the corresponding workflow under `frontend/src/...`.
5. Keep the backend and frontend connected through REST endpoints and JSON contracts.

### Good project conventions to follow

- Use package-based organization by feature/domain, e.g.:
  - `com.pashe.app.user`
  - `com.pashe.app.emergency`
  - `com.pashe.app.healthcare`
  - `com.pashe.app.shared`
- Use Flyway migrations for all database schema changes.
- Prefer clear, REST-like URL structure under `/api/...`.
- Keep frontend logic simple and aligned to the API contract.
- Add tests for business logic and integration points rather than only smoke tests.

## 7) Important notes for the coding agent

- There is no complete feature implementation yet; the app is a starter for the product.
- The backend and frontend are intentionally separate and should evolve independently but remain API-aligned.
- The initial health check is already implemented and can be used as a reference pattern.
- This repo is a good place to begin building the first real domain features without needing to remove a lot of legacy code.
- Any database schema addition should be added via new Flyway SQL migration files, not by editing the initial placeholder migration.

## 8) Suggested starting points

If the goal is to begin a new feature, the most natural starting points are:

- `backend/src/main/java/com/pashe/app/shared/health/HealthController.java` as a reference for controller patterns
- `backend/src/main/resources/application.yaml` for config conventions
- `backend/src/main/resources/db/migration/` for schema evolution
- `frontend/src/App.tsx` as the current UI scaffold

## 9) Minimal validation commands

After implementing a feature, validate with the smallest relevant checks:

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm run build
```

You can also run the backend and frontend dev servers together while implementing a feature.

## 10) Summary

This codebase is a clean starter project for Pashe: backend + frontend + PostgreSQL, with a health check already wired and a basic landing page in place. The feature work should focus on real user-facing healthcare flows and persisted data models, with Flyway-based backend schema changes and React-based UI work in parallel.
