# Finance Dashboard Backend — Zorvyn Assessment

A role-based finance data management backend built with **Spring Boot 3**, **JWT authentication**, and **MySQL**. Designed to serve a finance dashboard system where users interact with financial records based on their assigned role.

---

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
├── Exception/            # Global exception handler
└── Dto/                  # Shared data transfer objects

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

### Step 2 — Configure Database Credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_db
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.port=8081
```

> Tables are auto-created by Hibernate on first run (`ddl-auto=update`). No SQL migration scripts are needed.

### Step 3 — Run the Application

```bash
./mvnw spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw clean package -DskipTests
java -jar target/SpringSecurityBasicAuth-0.0.1-SNAPSHOT.jar
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
POST /api/login
  → Returns JWT token

All other requests
  → Include header: Authorization: Bearer <token>
```

**Token expiry:** 1 hour from time of issue.

**How it works:**

1. Client calls `POST /api/login` with username and password.
2. Server validates credentials and returns a signed JWT.
3. Client includes the JWT in every subsequent request via the `Authorization` header.
4. The `JWTAuthenticationFilter` validates the token and loads the user's role into the security context.
5. Method-level `@PreAuthorize` checks enforce role-based access.

---

## Role & Access Control

Three roles are supported. Access is enforced at the controller level using `@PreAuthorize` annotations.

| Action | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| View dashboard summary | ✅ | ✅ | ✅ |
| View category / monthly trends | ✅ | ✅ | ✅ |
| View recent transactions | ✅ | ✅ | ✅ |
| View financial records (paginated) | ❌ | ✅ | ✅ |
| Create financial record | ❌ | ❌ | ✅ |
| Update financial record | ❌ | ❌ | ✅ |
| Delete financial record | ❌ | ❌ | ✅ |
| Create user | ❌ | ❌ | ✅ |
| List all users | ❌ | ❌ | ✅ |
| Update user role | ❌ | ❌ | ✅ |
| Update user status | ❌ | ❌ | ✅ |
| Deactivate (soft-delete) user | ❌ | ❌ | ✅ |

**Inactive users** are blocked from logging in — `isEnabled()` on the `User` entity returns `false` when `status = INACTIVE`, and Spring Security automatically rejects their authentication.

---

## API Reference

Base URL: `http://localhost:8081`

All protected endpoints require the header:
```
Authorization: Bearer <your_jwt_token>
```

---

### Auth

#### `POST /api/login`
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

#### `POST /api/users`
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

#### `GET /api/users`
List all active users.

**Response `200 OK`:**
```json
[
  { "id": 1, "username": "admin", "role": "ADMIN" },
  { "id": 2, "username": "john_doe", "role": "ANALYST" }
]
```

> Note: INACTIVE users are excluded from this list.

---

#### `PATCH /api/users/{id}/role`
Update the role of a specific user. Cannot change role of an INACTIVE user.

**Query Param:** `role=VIEWER`

**Response `200 OK`:**
```json
{ "id": 2, "username": "john_doe", "role": "VIEWER" }
```

**Response `403 Forbidden`** — if user is inactive.
**Response `404 Not Found`** — if user does not exist.

---

#### `PATCH /api/users/{id}/status`
Activate or deactivate a user.

**Query Param:** `status=INACTIVE`

Allowed values: `ACTIVE`, `INACTIVE`

**Response `200 OK`:**
```json
{ "id": 2, "username": "john_doe", "role": "VIEWER" }
```

---

#### `DELETE /api/users/{id}`
Soft-deactivates a user by setting their status to `INACTIVE`. The user record is retained in the database; they simply cannot log in.

**Response `204 No Content`**

---

### Financial Records

#### `POST /api/records`
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

- `amount`: Required, must be positive.
- `type`: Required — `INCOME` or `EXPENSE`
- `category`: Required
- `date`: Optional — defaults to today if not provided
- `note`: Optional

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

#### `GET /api/records`
> Requires role: **ANALYST** or **ADMIN**

Retrieve paginated financial records with optional filters.

**Query Parameters (all optional):**

| Parameter | Type | Description |
|---|---|---|
| `type` | `INCOME` or `EXPENSE` | Filter by record type |
| `category` | String | Filter by category (case-insensitive) |
| `from` | Date (`yyyy-MM-dd`) | Start date filter |
| `to` | Date (`yyyy-MM-dd`) | End date filter |
| `search` | String | Keyword search across note and category |
| `page` | Integer | Page number (default: 0) |
| `size` | Integer | Page size (default: 10) |
| `sort` | String | Sort field (default: `date`) |

**Example Request:**
```
GET /api/records?type=EXPENSE&category=rent&from=2024-01-01&to=2024-12-31&page=0&size=5
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

#### `PUT /api/records/{id}`
> Requires role: **ADMIN**

Update an existing financial record.

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

#### `DELETE /api/records/{id}`
> Requires role: **ADMIN**

Permanently delete a financial record.

**Response `204 No Content`**
**Response `404 Not Found`** — if record ID doesn't exist.

---

### Dashboard & Analytics

> All dashboard endpoints are accessible to **all roles** (VIEWER, ANALYST, ADMIN).

#### `GET /api/dashboard`
Returns a complete dashboard summary in a single call — ideal for loading the main dashboard screen.

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

#### `GET /api/dashboard/summary/category`
Category-wise net totals.

**Response `200 OK`:**
```json
{
  "Salary": 60000.00,
  "Rent": -18000.00,
  "Groceries": -8000.00
}
```

---

#### `GET /api/dashboard/summary/monthly`
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

#### `GET /api/dashboard/summary/recent`
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
| `amount` | Double | Must be positive |
| `type` | Enum | `INCOME` or `EXPENSE` |
| `category` | String | Required |
| `date` | Date | Defaults to creation date |
| `note` | String | Optional description |
| `createdBy` | User | Many-to-one FK to User |

---

## Error Handling

All errors return a consistent JSON structure with an appropriate HTTP status code.

| Scenario | HTTP Status | Response |
|---|---|---|
| Invalid/missing request fields | `400 Bad Request` | Field-level validation errors |
| Wrong credentials | `401 Unauthorized` | `{ "error": "Invalid username or password" }` |
| Missing or invalid JWT | `403 Forbidden` | Spring Security default |
| Role insufficient | `403 Forbidden` | Spring Security default |
| Resource not found | `404 Not Found` | `{ "error": "Record not found" }` |
| Username already exists | `409 Conflict` | `{ "error": "User already exists" }` |
| Modify inactive user | `403 Forbidden` | `{ "error": "Cannot modify inactive user" }` |

**Validation error example (`400 Bad Request`):**
```json
{
  "amount": "Amount must be greater than or equal zero",
  "type": "Type is required"
}
```

---

## Assumptions Made

1. **Only ADMIN can create users.** There is no public self-registration endpoint. This was an intentional design decision to ensure a controlled user onboarding process in a finance system where role assignment is security-sensitive.

2. **VIEWER cannot view raw financial records.** VIEWERs have access to all dashboard and analytics endpoints (totals, trends, summaries) but not the paginated record listing. The rationale is that dashboard data is aggregated and anonymised, while raw records may contain sensitive transaction-level details.

3. **Delete is a soft-delete for users, hard-delete for records.** Users are deactivated (`INACTIVE`) rather than removed to preserve data integrity and audit trails. Financial records are permanently deleted when explicitly requested.

4. **Record date defaults to today** if not provided on creation. On update, the date is only changed if a new date is explicitly sent in the request body.

5. **A default admin user is seeded on startup** (`admin` / `admin123`) so the system can be used immediately without manual database setup. This is intended for development/assessment purposes.

6. **JWT expiry is set to 1 hour.** No refresh token mechanism is implemented. Token refresh was considered an optional enhancement beyond the scope of this assignment.

7. **Category is a free-text string**, not a managed enum or lookup table. This keeps the schema flexible. A categories table could be added as a future enhancement.

---

## Tradeoffs Considered

### Stateless JWT vs Sessions
Stateless JWT was chosen because it is better suited for API-first architectures and works seamlessly with `@PreAuthorize` in Spring Security. The tradeoff is that tokens cannot be revoked before expiry unless a blocklist is maintained — which was not implemented here for simplicity.

### In-memory filtering vs JPQL query
Filtering in `RecordsRepository` uses a single parameterised JPQL query with optional conditions. This is more efficient than loading all records and filtering in Java — especially with pagination — at the cost of a slightly more complex query string.

### `findAll()` in DashboardService
Dashboard aggregations (`getCategorySummary`, `getMonthlySummary`) call `findAll()` and process records in-memory using Java streams. For a small dataset this is fine, but at scale this would be replaced with `@Query` aggregate functions or database views. This was a conscious simplicity tradeoff for the assessment.

### Hard-delete for records
Financial records are hard-deleted rather than soft-deleted. In a real production finance system, soft-delete (with a `deleted_at` field) would be preferred for compliance and audit purposes. A `deleted` flag was omitted here to keep the schema simple.

### BCrypt strength 12
Password encoding uses BCrypt with strength 12, which is more secure than the default (10) but slightly slower. This is a deliberate security-first decision.

---

## API Documentation (Swagger)

### Option 1 — Live Swagger UI (when app is running)

```
http://localhost:8081/swagger-ui/index.html
```

Click **Authorize** (top right), enter `Bearer <your_token>` to test protected endpoints directly.

### Option 2 — Import OpenAPI spec into Postman

The full OpenAPI 3.1 specification is included in the repository:

```
docs/openapi.json
```

**To import into Postman:**
1. Open Postman → click **Import**
2. Select `docs/openapi.json`
3. All endpoints will be imported as a collection with schema definitions

**To import into Swagger UI (standalone):**
1. Go to [https://editor.swagger.io](https://editor.swagger.io)
2. Click **File → Import File**
3. Upload `docs/openapi.json`

The spec includes all request/response schemas, query parameters, authentication configuration (`BearerAuth`), and enum definitions for `role`, `status`, and `type`.