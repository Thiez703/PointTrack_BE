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

# Run a single test class
./mvnw test -Dtest=ShiftServiceTest

# Run a single test method
./mvnw test -Dtest=ShiftServiceTest#testConflictDetection

# Docker helpers
docker compose logs -f app
docker compose down
```

## Environment Setup

Copy `.env` from the README template. Key variables:
- `MYSQL_PORT=3307` (not default 3306 — Docker maps to 3307 locally)
- `JWT_SECRET` — hex-encoded 32+ char secret
- `CORS_ALLOWED_ORIGINS` — comma-separated frontend origins
- `GOOGLE_MAPS_API_KEY` — required for geocoding and travel time calculation
- `GEOFENCE_RADIUS_METERS` — default 100m for GPS check-in validation

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
| Repositories | `repository/` | Spring Data JPA, 15 interfaces with custom JPQL |
| Entities | `entity/` | JPA entities, all extend `BaseEntity` (audit fields) |
| DTOs | `dto/` | Request/response objects, organized by domain subdirectory |
| Security | `security/` | JWT filter, custom UserDetails, authorization aspects |
| Config | `config/` | SecurityConfig, CORS, Swagger, Redis, DataSeeder |
| Utils | `utils/` | JWT utility, Cookie utility, GpsUtils, MessagesUtils |
| Common | `common/` | `AuthUtils.getUserDetail()` — get current authenticated user |

### Key Services

- **ShiftService** — core shift lifecycle: create, assign, conflict check, status transitions; includes copy-week feature
- **ConflictCheckerService** — detects shift time/buffer overlaps before assignment
- **AttendanceService** — check-in/out with GPS validation, geofence enforcement, explanation requests
- **ServicePackageService** — manages recurring service packages and auto-generates Shifts from recurrence patterns
- **AuthService** — login, logout, token refresh, first-time password change, OTP-based password reset
- **PasswordService** — OTP generation/validation for password reset (via SmsService)
- **SmsService** — sends OTP SMS messages
- **FileStorageService** — handles attendance photo uploads (stores to local filesystem or cloud)
- **SalaryLevelService** — manages salary tiers and records history on change
- **GeocodingService** — calls Google Maps API to resolve addresses to lat/lng for customers
- **TravelTimeService** — estimates travel time between locations via Google Maps
- **CustomerImportService / EmployeeImportService** — Excel (Apache POI) bulk import
- **SchedulingSettingsService** — reads/writes `SystemSetting` key-value config (grace period, penalty rules, travel buffer)

## Security & Authorization

- **JWT** stored in cookies (not Authorization header). `JWTFilter` validates every request.
- **Token blacklist** in Redis — used for logout/revocation. JWT expiry: 1h; refresh: 7 days.
- Two-layer authorization:
  1. `@PreAuthorize` / role checks via Spring Security (`ROLE_ADMIN`, `ROLE_USER`)
  2. Fine-grained `@RequirePermission("PERMISSION_CODE")` custom annotation (AOP-based)
- Get current user: `AuthUtils.getUserDetail()` returns `CustomUserDetails`
- Cloudflare Turnstile CAPTCHA integration on login endpoint

## Key Domain Concepts

- **Shift** — fixed-duration work assignment for an employee at a customer location; status lifecycle: `DRAFT → PUBLISHED → IN_PROGRESS → COMPLETED/CANCELLED`
- **ShiftTemplate** — reusable shift definition (start/end time, OT multiplier); referenced by both Shifts and ServicePackages
- **WorkSchedule** — daily work assignment linking User + ShiftTemplate + Customer with a OneToOne to AttendanceRecord
- **ServicePackage** — recurring service with recurrence pattern (days/times stored as JSON); generates Shifts automatically
- **AttendanceRecord** — check-in/out with GPS coordinates; validated against customer location (geofence radius: 100m default); tracks late/early minutes
- **AttendancePhoto** — photos captured at check-in/out with GPS metadata (type: CHECK_IN / CHECK_OUT)
- **AttendanceAuditLog** — immutable audit trail written when Admin edits an attendance record
- **ExplanationRequest** — submitted when check-in is late, GPS mismatches, or early checkout; status: `PENDING → APPROVED/REJECTED`
- **SystemSetting** — key-value config table (e.g., `GRACE_PERIOD_MINUTES`, penalty rules); managed via `SchedulingSettingsController`
- **SalaryLevel / SalaryLevelHistory** — tiered salary with change history when Admin updates employee level

### Entity Relationships Summary

```
User ──── Role ──ManyToMany── Permission
User ──── SalaryLevel
User ──── Shift (employee)
User ──── WorkSchedule ──── AttendanceRecord ──── AttendancePhoto
                                               └── ExplanationRequest
Customer ── Shift, ServicePackage, WorkSchedule (has lat/lng for GPS)
Shift ──── ShiftTemplate, ServicePackage
ServicePackage ──── ShiftTemplate, Customer, User
```

## Development Conventions

- **Naming:** camelCase (Java), snake_case (DB columns), kebab-case (URL paths)
- **Response wrapper:** use the project's standard `ApiResponse<T>` for all endpoints
- **Custom exceptions:** throw project-specific exceptions (in `exception/` package); global handler `ApiExceptionHandler` catches all. Key exceptions: `NotFoundException` (404), `BadRequestException` (400), `ConflictException` (409), `ShiftAssignException` (400), `Forbidden` (403)
- **New feature workflow:** Entity → Repository → DTO → Service → Controller
- **Enums:** all in `entity/enums/`; key ones: `ShiftStatus`, `AttendanceStatus`, `ExplanationStatus`, `ShiftType`, `PackageStatus`
- **DB migrations:** SQL files in `src/main/resources/db/` using Flyway `V{n}__` naming (e.g., `V8__...sql`); `ddl-auto: update` in dev but migrations track schema changes
- **Permissions:** new permissions must be added to the `Permission` seed data in `DataSeeder` and referenced via `@RequirePermission`
- **Soft delete:** entities use `BaseEntity` audit fields; check existing pattern before implementing delete logic
- **JSON list storage:** `common/StringListConverter` is a JPA `AttributeConverter` that serializes `List<String>` to/from a JSON string column — used for recurrence day/time patterns in `ServicePackage`

## Testing

- Framework: JUnit 5 + Spring Boot Test; uses H2 in-memory DB (no MySQL required for tests)
- Tests in `src/test/java/com/teco/pointtrack/`
- Key test classes: `ConflictCheckerServiceTest`, `EmployeeServiceTest`, `CustomerServiceTest`, `GeocodingServiceTest`, `PasswordServiceTest`, `EmployeeImportServiceTest`, `CustomerImportServiceTest`
- Mock external APIs (Google Maps, Turnstile) in tests; real DB connections use H2

## API Documentation

Swagger UI available at `http://localhost:8080/api/swagger-ui.html` when running locally.

Additional API docs in `docs/api-customer.md` and `docs/api-shift.md`.
