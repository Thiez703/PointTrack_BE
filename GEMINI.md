# PointTrack — Hệ thống Chấm công & Quản lý Ca

PointTrack is a specialized backend system designed for home service businesses (e.g., baby bathing, cleaning, electrical/water repair). It focuses on tracking employees who move between customer locations, requiring strict GPS verification and flexible shift management.

## Project Overview

- **Main Technologies:** Java 21, Spring Boot 3.5.5, MySQL 8, Redis, JJWT (0.12.5), Spring Data JPA, Hibernate.
- **Architecture:** Standard N-tier architecture:
    - `controller/`: REST API endpoints.
    - `service/`: Business logic.
    - `repository/`: Data access layer (Spring Data JPA).
    - `entity/`: JPA entities (MySQL).
    - `dto/`: Data Transfer Objects for requests and responses.
- **Key Features:**
    - **GPS Fencing:** Verification of check-in/out within a 50m radius of the customer's location.
    - **Shift Management:** Support for fixed duration shifts, drag-and-drop scheduling, and service packages.
    - **Exception Handling:** Automated handling for late check-ins, early check-outs, and GPS mismatches.
    - **Security:** Spring Security with JWT, custom RBAC (Role-Based Access Control) with fine-grained permissions, and Redis-based token blacklisting.
    - **Audit Logging:** Comprehensive tracking of attendance record changes.

## Building and Running

### Environment Requirements
- JDK 21
- Docker Desktop (for MySQL and Redis)
- Maven 3.9+ (or use the provided `./mvnw`)

### Setup & Run
1.  **Start Dependencies:**
    ```bash
    docker compose up db redis -d
    ```
2.  **Configuration:**
    - Create a `.env` file at the root (refer to `README.md` for a template).
    - Default profile is `dev`. Configuration is located in `src/main/resources/application.yml` and `application-dev.yml`.
3.  **Build Project:**
    ```bash
    ./mvnw clean install
    ```
4.  **Run Application:**
    ```bash
    ./mvnw spring-boot:run
    ```
5.  **API Documentation:**
    - Swagger UI: `http://localhost:8080/api/swagger-ui.html`

## Development Conventions

### Coding Style & Standards
- **Naming:** `camelCase` for Java variables/methods, `PascalCase` for classes, `snake_case` for database columns, and `kebab-case` for API URLs.
- **Entity Guidelines:** All entities should extend `BaseEntity` to inherit auditing fields (`createdAt`, `updatedAt`, `createdByUserId`, `updatedByUserId`).
- **Response Wrapper:**
    - Use `ApiResponse.success(data, message)` for returning data.
    - Use `MessageResponse` for simple success/failure actions.
- **Exceptions:** Throw specific exceptions found in `com.teco.pointtrack.exception` (e.g., `BadRequestException`, `NotFoundException`, `Forbidden`). These are automatically mapped to HTTP status codes by `ApiExceptionHandler`.

### Security & Authorization
- **Authentication:** Use Bearer Token (JWT).
- **Authorization:**
    - Use `@PreAuthorize("hasRole('ADMIN')")` for role-based access.
    - Use `@RequirePermission("PERMISSION_CODE")` for granular permission checks on controllers or methods.
- **Current User Access:** Use `AuthUtils.getUserDetail()` to retrieve the currently authenticated user's details.

### Implementation Workflow
When adding new features, follow this sequence:
1.  Define/Update **Entity** in `entity/`.
2.  Create/Update **Repository** in `repository/`.
3.  Define **DTOs** in `dto/` for requests and responses.
4.  Implement business logic in **Service** in `service/`.
5.  Expose endpoints in **Controller** in `controller/`, ensuring proper versioning (e.g., `/v1/...`) and permission annotations.

### Testing
- Automated tests are located in `src/test/java`.
- Run tests using: `./mvnw test`
- Use H2 database for unit/integration testing where applicable.
