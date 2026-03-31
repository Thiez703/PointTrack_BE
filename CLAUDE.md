# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean install
./mvnw clean package -DskipTests

# Run (start DB/Redis via Docker, app via Maven)
docker compose up db redis -d
./mvnw spring-boot:run

# Run full stack (app + DB + Redis in Docker)
docker compose up --build -d

# Tests
./mvnw test

# Docker helpers
docker compose logs -f app
docker compose down
```

## Environment Setup

Copy `.env` from the README template. Key variables:
- `MYSQL_PORT=3307` (not default 3306 — Docker maps to 3307 locally)
- `JWT_SECRET` — hex-encoded 32+ char secret
- `CORS_ALLOWED_ORIGINS` — comma-separated frontend origins

Active Spring profiles: `dev` (local dev uses `application-dev.yml`), `prod` (uses `application-prod.yml`).

Default admin account: `admin@pointtrack.com` / `Admin@123`

## Architecture

**Spring Boot 3.5.5 + Java 21 REST API** for home service business management with GPS-based attendance tracking.

Package root: `com.teco.pointtrack`

Standard layered architecture: `controller → service → repository → entity`

| Layer | Package | Notes |
|-------|---------|-------|
| Controllers | `controller/` | REST endpoints, all under `/api` context path |
| Services | `service/` | Business logic |
| Repositories | `repository/` | Spring Data JPA, 19 interfaces with custom JPQL |
| Entities | `entity/` | JPA entities, all extend `BaseEntity` (audit fields) |
| DTOs | `dto/` | Request/response objects |
| Security | `security/` | JWT filter, custom UserDetails, authorization aspects |
| Config | `config/` | SecurityConfig, CORS, Swagger, Redis |
| Utils | `utils/` | JWT utility, Cookie utility |
| Common | `common/` | `AuthUtils.getUserDetail()` — get current authenticated user |

## Security & Authorization

- **JWT** stored in cookies (not Authorization header). `JWTFilter` validates every request.
- **Token blacklist** in Redis — used for logout/revocation.
- Two-layer authorization:
  1. `@PreAuthorize` / role checks via Spring Security (`ROLE_ADMIN`, `ROLE_USER`)
  2. Fine-grained `@RequirePermission("PERMISSION_CODE")` custom annotation (AOP-based)
- Get current user: `AuthUtils.getUserDetail()` returns `CustomUserDetails`

## Key Domain Concepts

- **Shift** — fixed-duration work assignment for an employee at a customer location
- **ShiftTemplate** — reusable shift definition
- **AttendanceRecord** — check-in/out with GPS coordinates; validated against customer location (geofence radius: 100m default)
- **ExplanationRequest** — submitted when check-in is late, GPS mismatches, or early checkout
- **ServicePackage** — recurring service assigned to a customer
- **SalaryLevel / SalaryLevelHistory** — tiered salary with change history

## Development Conventions

- **Naming:** camelCase (Java), snake_case (DB columns), kebab-case (URL paths)
- **Response wrapper:** use the project's standard `ApiResponse<T>` for all endpoints
- **Custom exceptions:** throw project-specific exceptions (in `exception/` package); global handler exists
- **New feature workflow:** Entity → Repository → DTO → Service → Controller
- **DB migrations:** SQL files in `src/main/resources/db/` using Vn__ naming (e.g., `V8__...sql`); `ddl-auto: update` in dev but migrations track schema changes
- **Permissions:** new permissions must be added to the `Permission` seed data and referenced via `@RequirePermission`

## API Documentation

Swagger UI available at `http://localhost:8080/api/swagger-ui.html` when running locally.

Additional API docs in `docs/api-customer.md` and `docs/api-shift.md`.
