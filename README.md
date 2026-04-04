## Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Running Locally](#setup--running-locally)
- [Authentication Flow](#authentication-flow)
- [Role & Access Control](#role--access-control)
- [API Reference](#api-reference)
  - [Auth](#auth)
  - [User Management](#user-management)
  - [Financial Records](#financial-records)
  - [Dashboard & Analytics](#dashboard--analytics)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Assumptions Made](#assumptions-made)
- [Tradeoffs Considered](#tradeoffs-considered)
- [API Documentation (Swagger)](#api-documentation-swagger)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 3.5.3 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |

---

## Project Structure

```
src/main/java/com/adarsh/zorvyn/
│
├── Controllers/          # REST endpoints (Auth, User, Record, Dashboard)
├── Service/              # Business logic layer
├── Repository/           # Spring Data JPA interfaces
├── Entity/               # JPA entities (User, Record, Role, Status, Type)
├── Request/              # Incoming request DTOs with validation
├── Response/             # Outgoing response DTOs
├── Config/               # Security config, seed admin user
├── Filters/              # JWT authentication filter
├── Utils/                # JWT utility (generate, validate, extract)
└── Exception/            # Global exception handler

docs/
└── openapi.json          # Full OpenAPI 3.1 specification (importable in Postman / Swagger UI)
```

---

## Setup & Running Locally

### Prerequisites

- Java 24+
- Maven 3.8+
- MySQL 8 running locally

### Step 1 — Create the Database

```sql
CREATE DATABASE finance_db;
```

### Step 2 — Configure Environment Variables

This project uses environment variables for all sensitive configuration. No credentials are hardcoded.

Set the following environment variables before running:

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | JDBC connection string | `jdbc:mysql://localhost:3306/finance_db` |
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `yourpassword` |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) | `a-very-long-and-secure-secret-key-here` |
| `SERVER_PORT` | Server port (optional, defaults to `8081`) | `8081` |

**On Linux / Mac:**
```bash
export DB_URL=jdbc:mysql://localhost:3306/finance_db
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export JWT_SECRET=a-very-long-and-secure-secret-key-here
```

**On Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/finance_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="yourpassword"
$env:JWT_SECRET="a-very-long-and-secure-secret-key-here"
```

> Tables are auto-created by Hibernate on first run (`ddl-auto=update`). No SQL migration scripts are needed.

### Step 3 — Run the Application

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/FinanceDashboard-0.0.1-SNAPSHOT.jar
```

### Step 4 — Default Admin User

On first startup, a seed admin user is automatically created if it does not exist:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `admin123` |
| Role | `ADMIN` |

Use these credentials to log in and obtain a JWT token before calling any protected endpoints.

---

## Authentication Flow

This API uses **stateless JWT-based authentication**. There are no sessions or cookies.

```
POST /api/v1/login
  → Returns JWT token

All other requests
  → Include header: Authorization: Bearer <token>
```

**Token expiry:** 1 hour from time of issue (configurable via `jwt.expiration` in milliseconds).

**How it works:**

1. Client calls `POST /api/v1/login` with username and password.
2. Server validates credentials and returns a signed JWT.
3. Client includes the JWT in every subsequent request via the `Authorization` header.
4. The `JWTAuthenticationFilter` validates the token and loads the user's role into the security context.
5. Method-level `@PreAuthorize` checks enforce role-based access on every endpoint.

---

## Role & Access Control

Three roles are supported. Access is enforced at the controller level using `@PreAuthorize` annotations.

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| View dashboard summary | ✅ | ✅ | ✅ |
| View category / monthly trends | ✅ | ✅ | ✅ |
| View recent transactions | ✅ | ✅ | ✅ |
| View financial records (paginated, filtered) | ❌ | ✅ | ✅ |
| Create financial record | ❌ | ❌ | ✅ |
| Update financial record | ❌ | ❌ | ✅ |
| Delete financial record | ❌ | ❌ | ✅ |
| Create user | ❌ | ❌ | ✅ |
| List active users | ❌ | ❌ | ✅ |
| List inactive users | ❌ | ❌ | ✅ |
| Update user role | ❌ | ❌ | ✅ |
| Update user status | ❌ | ❌ | ✅ |
| Soft-delete user | ❌ | ❌ | ✅ |

**Inactive users** are blocked from logging in — `isEnabled()` on the `User` entity returns `false` when `status = INACTIVE`, and Spring Security automatically rejects their authentication attempt.

---

## API Reference

Base URL: `http://localhost:8081/api/v1`

All protected endpoints require the header:
```
Authorization: Bearer <your_jwt_token>
```

---

### Auth

#### `POST /api/v1/login`
Authenticate and receive a JWT token. This endpoint is public — no token required.

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response `401 Unauthorized`:**
```json
{
  "error": "Invalid username or password"
}
```

---

### User Management

> All user management endpoints require role: **ADMIN**

#### `POST /api/v1/users`
Create a new user. Only an authenticated admin can create users.

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securepass",
  "role": "ANALYST"
}
```

Allowed values for `role`: `ADMIN`, `ANALYST`, `VIEWER`

**Response `200 OK`:**
```json
{
  "id": 2,
  "username": "john_doe",
  "role": "ANALYST"
}
```

**Response `409 Conflict`** — if username already exists.

---

#### `GET /api/v1/users`
List all active users. INACTIVE users are excluded from this list.

**Response `200 OK`:**
```json
[
  { "id": 1, "username": "admin", "role": "ADMIN" },
  { "id": 2, "username": "john_doe", "role": "ANALYST" }
]
```

---

#### `GET /api/v1/users/inactive`
List all inactive (soft-deleted) users. Useful for admin audit and to identify users eligible for reactivation.

**Response `200 OK`:**
```json
[
  { "id": 3, "username": "old_user", "role": "VIEWER" }
]
```

---

#### `PATCH /api/v1/users/{id}/role`
Update the role of a specific user. Cannot change the role of an INACTIVE user.

**Query Param:** `role=VIEWER`

Allowed values: `ADMIN`, `ANALYST`, `VIEWER`

**Response `200 OK`:**
```json
{ "id": 2, "username": "john_doe", "role": "VIEWER" }
```

**Response `403 Forbidden`** — if user is inactive.

**Response `404 Not Found`** — if user does not exist.

---

#### `PATCH /api/v1/users/{id}/status`
Directly set a user's status. Can be used to reactivate a previously deactivated user.

**Query Param:** `status=INACTIVE`

Allowed values: `ACTIVE`, `INACTIVE`

**Response `200 OK`:**
```json
{ "id": 2, "username": "john_doe", "role": "VIEWER" }
```

---

#### `DELETE /api/v1/users/{id}`
Soft-deletes a user by setting their status to `INACTIVE`. The user record is retained in the database; they simply cannot log in. Use `PATCH /{id}/status?status=ACTIVE` to reactivate them.

**Response `204 No Content`**

**Response `404 Not Found`** — if user does not exist.

---

### Financial Records

#### `POST /api/v1/records`
> Requires role: **ADMIN**

Create a new financial record.

**Request Body:**
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-06-15",
  "note": "Monthly salary credit"
}
```

| Field | Required | Notes |
|---|---|---|
| `amount` | Yes | Must be greater than zero |
| `type` | Yes | `INCOME` or `EXPENSE` |
| `category` | Yes | Free-text string |
| `date` | No | Defaults to today if not provided |
| `note` | No | Optional description |

**Response `200 OK`:**
```json
{
  "id": 1,
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-06-15T00:00:00.000+00:00",
  "note": "Monthly salary credit"
}
```

---

#### `GET /api/v1/records`
> Requires role: **ANALYST** or **ADMIN**

Retrieve paginated financial records with optional filters. All filters are independent and can be freely combined.

**Query Parameters (all optional):**

| Parameter | Type | Description |
|---|---|---|
| `type` | `INCOME` or `EXPENSE` | Filter by record type |
| `category` | String | Filter by category (case-insensitive exact match) |
| `from` | Date (`yyyy-MM-dd`) | Records on or after this date |
| `to` | Date (`yyyy-MM-dd`) | Records on or before this date |
| `search` | String | Keyword search across note and category fields |
| `page` | Integer | Page number, zero-based (default: `0`) |
| `size` | Integer | Page size (default: `10`) |
| `sort` | String | Sort field (default: `date`) |

**Example Request:**
```
GET /api/v1/records?type=EXPENSE&category=rent&from=2024-01-01&to=2024-12-31&page=0&size=5
```

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": 3,
      "amount": 12000.00,
      "type": "EXPENSE",
      "category": "Rent",
      "date": "2024-03-01T00:00:00.000+00:00",
      "note": "March rent"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 5,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

---

#### `PUT /api/v1/records/{id}`
> Requires role: **ADMIN**

Update an existing financial record. If `date` is not included in the request body, the record's existing date is preserved unchanged.

**Request Body:**
```json
{
  "amount": 6000.00,
  "type": "INCOME",
  "category": "Freelance",
  "date": "2024-07-01",
  "note": "Consulting project payment"
}
```

**Response `200 OK`** — updated record object.

**Response `404 Not Found`** — if record ID doesn't exist.

---

#### `DELETE /api/v1/records/{id}`
> Requires role: **ADMIN**

Permanently delete a financial record.

**Response `204 No Content`**

**Response `404 Not Found`** — if record ID doesn't exist.

---

### Dashboard & Analytics

> All dashboard endpoints are accessible to **all authenticated roles** (VIEWER, ANALYST, ADMIN).

#### `GET /api/v1/dashboard`
Returns a complete dashboard summary in a single call — ideal for the initial page load of a dashboard frontend.

**Response `200 OK`:**
```json
{
  "totalIncome": 85000.00,
  "totalExpense": 42000.00,
  "netBalance": 43000.00,
  "categoryTotals": {
    "Salary": 60000.00,
    "Freelance": 25000.00,
    "Rent": -18000.00,
    "Groceries": -8000.00
  },
  "monthlyTrends": {
    "2024-01": 12000.00,
    "2024-02": 9500.00,
    "2024-03": -3000.00
  },
  "recentTransactions": [
    {
      "id": 12,
      "amount": 5000.00,
      "type": "INCOME",
      "category": "Salary",
      "date": "2024-06-30T00:00:00.000+00:00",
      "note": "June salary"
    }
  ]
}
```

**Notes on response fields:**
- `categoryTotals` — net value per category (income is positive, expense is negative)
- `monthlyTrends` — net value per month in `yyyy-MM` format, sorted chronologically
- `recentTransactions` — the 5 most recent records by date

---

#### `GET /api/v1/dashboard/summary/category`
Category-wise net totals. Income values are positive, expense values are negative.

**Response `200 OK`:**
```json
{
  "Salary": 60000.00,
  "Rent": -18000.00,
  "Groceries": -8000.00
}
```

---

#### `GET /api/v1/dashboard/summary/monthly`
Net balance grouped by month (`yyyy-MM`), sorted chronologically.

**Response `200 OK`:**
```json
{
  "2024-01": 12000.00,
  "2024-02": 9500.00,
  "2024-06": -3000.00
}
```

---

#### `GET /api/v1/dashboard/summary/recent`
The 5 most recent financial records, sorted by date descending.

**Response `200 OK`:**
```json
[
  {
    "id": 12,
    "amount": 5000.00,
    "type": "INCOME",
    "category": "Salary",
    "date": "2024-06-30T00:00:00.000+00:00",
    "note": "June salary"
  }
]
```

---

## Data Models

### User

| Field | Type | Notes |
|---|---|---|
| `id` | Integer | Auto-generated |
| `username` | String | Unique, required |
| `email` | String | Valid email format |
| `password` | String | BCrypt-encoded (strength 12) |
| `role` | Enum | `ADMIN`, `ANALYST`, `VIEWER` |
| `status` | Enum | `ACTIVE`, `INACTIVE` |

### Financial Record

| Field | Type | Notes |
|---|---|---|
| `id` | Integer | Auto-generated |
| `amount` | Double | Must be positive (> 0) |
| `type` | Enum | `INCOME` or `EXPENSE` |
| `category` | String | Required, free-text |
| `date` | Date | Defaults to creation date if not provided |
| `note` | String | Optional description |
| `createdBy` | User | Many-to-one FK to User |

---

## Error Handling

All errors return a consistent JSON structure with an appropriate HTTP status code.

| Scenario | HTTP Status | Response Body |
|---|---|---|
| Invalid / missing request fields | `400 Bad Request` | Field-level validation errors map |
| Wrong credentials | `401 Unauthorized` | `{ "error": "Invalid username or password" }` |
| Missing or expired JWT token | `403 Forbidden` | Spring Security default |
| Insufficient role for endpoint | `403 Forbidden` | Spring Security default |
| Modifying an inactive user | `403 Forbidden` | `{ "error": "Cannot modify inactive user" }` |
| Resource not found | `404 Not Found` | `{ "error": "Record not found" }` |
| Username already exists | `409 Conflict` | `{ "error": "User already exists" }` |

**Validation error example (`400 Bad Request`):**
```json
{
  "amount": "Amount must be greater than zero",
  "type": "Type is required"
}
```

---

## Assumptions Made

1. **Only ADMIN can create users.** There is no public self-registration endpoint. This is an intentional design decision — in a finance system, user onboarding must be controlled because the role assigned at creation directly determines what data the user can access. An unchecked registration endpoint would be a security risk.

2. **VIEWER cannot view raw financial records.** VIEWERs have access to all dashboard and analytics endpoints (totals, trends, summaries) but not the paginated record listing. Dashboard data is aggregated and does not expose individual transaction details, whereas the records endpoint returns full transaction-level data which may be sensitive.

3. **Delete is a soft-delete for users, hard-delete for records.** Users are deactivated (`INACTIVE`) rather than removed — this preserves the `createdBy` relationship on financial records and maintains a user audit trail. Financial records are permanently deleted on request, as the assignment did not indicate a compliance or retention requirement.

4. **Inactive users can be viewed and reactivated.** The `GET /api/v1/users/inactive` endpoint lets admins see all deactivated users, and `PATCH /{id}/status?status=ACTIVE` can restore them. This makes soft-delete a reversible operation rather than a permanent action.

5. **Record date defaults to today** if not provided on creation. On update, the date is only modified if a new date is explicitly included in the request body — otherwise the existing date is preserved unchanged.

6. **A default admin user is seeded on startup** (`admin` / `admin123`) so the system is immediately usable without manual database setup. This is intended for development and assessment purposes only.

7. **JWT expiry is set to 1 hour.** No refresh token mechanism is implemented. Token refresh was considered out of scope for this assessment.

8. **Category is a free-text string**, not a managed lookup table. This keeps the schema simple and flexible. A dedicated `Category` entity could be added in a production system to enforce consistency.

---

## Tradeoffs Considered

### Stateless JWT vs Sessions
Stateless JWT was chosen because it suits API-first architectures and integrates cleanly with Spring Security's `@PreAuthorize`. The tradeoff is that tokens cannot be revoked before expiry without maintaining a blocklist — which was not implemented here. For assessment scope, this is an acceptable simplification.

### Environment Variables for Configuration
All sensitive values (`DB_URL`, `DB_PASSWORD`, `JWT_SECRET`) are read from environment variables rather than hardcoded in `application.properties`. This follows the 12-factor app principle and ensures no credentials are accidentally committed to source control.

### JPQL Filtering vs In-memory Filtering
Record filtering uses a single parameterised JPQL query in `RecordsRepository` that accepts optional conditions. This pushes filtering down to the database, which is significantly more efficient than loading all records and filtering in Java — especially with pagination enabled. The tradeoff is a more complex query string, but the correctness and performance gain is worth it.

### `findAll()` in DashboardService
Dashboard aggregations (`getCategorySummary`, `getMonthlySummary`) currently load all records into memory and use Java streams for grouping. For a small dataset this works correctly and is easy to understand. At scale, this would be replaced with SQL `GROUP BY` aggregate queries or database views. This was a deliberate simplicity tradeoff and is transparently documented here.

### Hard-delete for Records
Financial records are permanently deleted rather than soft-deleted. In a real production finance system, a `deleted_at` field would be preferred for compliance. A `deleted` flag was omitted to keep the schema simple; the tradeoff is acknowledged here.

### BCrypt Strength 12
Password encoding uses BCrypt with cost factor 12, which is more resistant to brute-force attacks than the default (10), at the cost of slightly slower hashing. This is a deliberate security-first choice.

### Dedicated `softDeleteUser` Method in Service
The `DELETE /api/v1/users/{id}` endpoint calls a dedicated `softDeleteUser` method in `UserService` rather than reusing `updateUserStatus`. This makes the intent explicit in both the controller and service layer — a reader can immediately understand that the DELETE endpoint performs a non-destructive soft operation, without tracing shared method logic.

---

## API Documentation (Swagger)

### Option 1 — Live Swagger UI (when app is running)

```
http://localhost:8081/swagger-ui/index.html
```

Click **Authorize** (top right) and enter:
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```
This allows you to test all protected endpoints directly from the browser.

### Option 2 — Import OpenAPI spec into Postman

The full OpenAPI 3.1 specification is included in the repository:

```
docs/openapi.json
```

**To import into Postman:**
1. Open Postman → click **Import**
2. Select `docs/openapi.json`
3. All endpoints are imported as a collection with request bodies and schema definitions

**To view in Swagger UI (standalone, no running app needed):**
1. Go to [https://editor.swagger.io](https://editor.swagger.io)
2. Click **File → Import File**
3. Upload `docs/openapi.json`

The spec includes all request/response schemas, query parameters, authentication setup (`BearerAuth`), and enum definitions for `role`, `status`, and `type`.