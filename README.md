# Product API (Java Backend Assignment)

Spring Boot REST API for Product CRUD with JWT authentication, refresh token rotation, role-based authorization, validation, pagination, OpenAPI docs, tests, and Docker support.

## Tech Stack
- Java 17
- Spring Boot
- Spring Data JPA (Hibernate)
- MySQL (runtime) and H2 (tests)
- Spring Security (JWT + refresh token)
- JUnit 5 + Mockito + MockMvc
- SpringDoc OpenAPI (Swagger UI)
- Docker + Docker Compose

## Architecture
- `controller`: API endpoints (`/api/v1/auth`, `/api/v1/products`)
- `service`: business logic (`AuthService`, `ProductService`, `RefreshTokenService`)
- `security`: JWT creation/validation, auth filter, security handlers
- `repository`: JPA repositories
- `entity`: DB models (`Product`, `Item`, `User`, `RefreshToken`)
- `exception`: global standardized error responses
- `dto`: request/response contracts and pagination/error payloads

## API Base URL
- `http://localhost:8080/api/v1`

## Core Endpoints
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /products`
- `GET /products/{id}`
- `POST /products` (ADMIN)
- `PUT /products/{id}` (ADMIN)
- `DELETE /products/{id}` (ADMIN)
- `GET /products/{id}/items`

## Roles
- `ROLE_ADMIN`: full product CRUD
- `ROLE_USER`: read-only product access

Default bootstrap admin:
- Username: `admin`
- Password: `Admin@12345`

## Run Locally
1. Configure MySQL and set env vars (or use defaults in `application.properties`).
2. Start app:
```bash
./mvnw spring-boot:run
```
3. Open Swagger UI:
- `http://localhost:8080/swagger-ui.html`

## Run Tests
```bash
./mvnw test
```
Tests use H2 (`application-test.properties`).

## Run With Docker Compose
```bash
docker compose up --build
```

## Notes
- Error responses are standardized with timestamp/status/message/path.
- Product list supports pagination via `page`, `size`, `sortBy`, `direction`.
- Refresh token endpoint rotates token and invalidates the previous one.
- HTTPS enforcement can be enabled via `REQUIRE_HTTPS=true`.
