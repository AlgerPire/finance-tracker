# Finance Tracker – Backend Server

A RESTful Spring Boot API for managing personal finance accounts and transactions, with JWT-based authentication and role-based access control.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
  - [Option A – Docker Compose (recommended)](#option-a--docker-compose-recommended)
  - [Option B – Manual / Maven](#option-b--manual--maven)
- [API Documentation (Swagger)](#api-documentation-swagger)
- [Key Design Decisions](#key-design-decisions)

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 21 |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |
| PostgreSQL | 16 (local install **or** via Docker) |
| Docker + Docker Compose | Any recent version (optional, for Option A) |

---

## Running Locally

### Option A – Docker Compose (recommended)

Everything (Postgres + the app) runs in containers. No local JDK or database install required.

**1. Copy the environment file and fill in your values.**

```bash
cp .env.example .env
```

At minimum, change `DB_PASSWORD`, `JWT_SECRET`, and the `ADMIN_*` credentials.

**2. Start the stack.**

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080` (or the port set by `APP_PORT` in `.env`).

**3. Stop the stack.**

```bash
docker compose down
```

Add `-v` to also remove the Postgres volume: `docker compose down -v`

---

### Option B – Manual / Maven

**1. Start a local PostgreSQL instance** with a database named `finance-tracker` on port `5432`.

**2. Configure the application.**

The `dev` profile uses `localhost:5432/finance-tracker` with no extra setup. You can override credentials in `application.yaml` or pass them as environment variables.

**3. Run the application.**

```bash
# From backend-server/
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or build a JAR first:

```bash
./mvnw package -DskipTests
java -jar target/backend-server-*.jar --spring.profiles.active=dev
```

The API will be available at `http://localhost:8080`.

---

## API Documentation (Swagger)

Once the application is running, open the interactive API docs in your browser:

```
http://localhost:8080/swagger-ui/index.html
```

The full OpenAPI spec (JSON) is also available at:

```
http://localhost:8080/v3/api-docs
```

Use the **Authorize** button in Swagger UI to attach a JWT Bearer token and test protected endpoints.

---

## Key Design Decisions

### Architecture

Modular Monolithic Architecture (MMA) is used for the backend. For faster development, low coupling, and easier maintenance in the future.

The backend follows a classic **layered architecture**: Controller → Service → Repository → Entity.

- **Controllers** handle HTTP and input validation only.
- **Services** (interface + `impl` pairs) contain all business logic.
- **Repositories** are Spring Data JPA interfaces; no hand-written SQL queries.
- **MapStruct mappers** convert between entities and DTOs, keeping the API contract decoupled from the database model.
- DTOs are split into separate `request` and `response` packages for clarity.

### API Surface

| Area | Base path | Who can access |
|------|-----------|----------------|
| Auth (sign-up, sign-in, refresh, logout) | `/api/auth/**` | Public |
| User accounts & transactions | `/api/accounts`, `/api/transactions` | Authenticated (`USER` role) |
| Admin – users, accounts, transactions | `/api/admin/**` | `ADMIN` role only |

### Security

- **JWT** (JJWT) is used for stateless authentication. Tokens are delivered as **HTTP-only cookies** and optionally via the `Authorization: Bearer` header for clients that cannot use cookies.
- **Refresh tokens** are persisted in the database and rotated/invalidated on each use.
- **BCrypt** is used for password hashing.
- **Method-level authorization** (`@PreAuthorize`) enforces role checks on every service method.
- **CSRF protection** is disabled (appropriate for a stateless JWT API consumed by a SPA).
- **Rate limiting** (Bucket4j) is applied to all `/api/auth/**` endpoints keyed by client IP to prevent brute-force attacks.

### Database

- **PostgreSQL 16** is the target database.
- **Schema management** uses `spring.jpa.hibernate.ddl-auto: update` in development for convenience. A Flyway dependency is present for migrating to versioned scripts in the future.
- A **default admin user** and application roles are seeded automatically on first startup using `DatabaseSeeder` and values from the `finance_tracker.app.admin` configuration namespace (overridable via environment variables).

### Configuration Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Local development; connects to `localhost:5432`, verbose SQL logging |
| `prod` | Production; reads all secrets from environment variables (`DATABASE_URL`, `JWT_SECRET`, etc.) |


### What's Next?

- Request for money option, similar to Paypal, where users can request money from other users.
- Accounting reports, including balance sheets, income statements, and cash flow statements.
- Securing application with OAuth2.
- Implementing notifications (email, SMS, push, etc.) for important events (daily summary, account balance changes, etc.)
- Implementing Spring Batch for background processing and data import/export.