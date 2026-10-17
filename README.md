# Pesira Employee Records

A Spring Boot and PostgreSQL API, with JWT authentication, for managing employee records.

## Prerequisites

- JDK 25 or newer
- Docker Desktop
- Git

Commands below are for a bash shell (Git Bash on Windows).

## Setup

1. Copy the example environment file:

   ```bash
   cp .env.example .env
   ```

2. Generate a JWT secret and paste it into `JWT_SECRET` in `.env`:

   ```bash
   openssl rand -base64 48
   ```

   The backend refuses to start if the secret is missing or shorter than 32 bytes.

If port 5432 is already taken (for example by a local PostgreSQL), set `DB_PORT` in `.env` to a
free port and use the same port in `DB_URL`.

## Run

Start PostgreSQL, then the backend from the `backend/` folder (it reads `../.env`):

```bash
docker compose up -d
cd backend
./mvnw spring-boot:run
```

On Windows without bash, run `mvnw.cmd spring-boot:run` (PowerShell: `.\mvnw.cmd spring-boot:run`).

The API listens on `http://localhost:8080`. On first start, Flyway creates the schema and seeds
demo users and 60 sample employees.

To stop PostgreSQL, run `docker compose down`. Add `-v` to also delete the data.

## Demo logins

| Email               | Password    | Role  |
|---------------------|-------------|-------|
| `admin@example.com` | `Admin123!` | ADMIN |
| `user@example.com`  | `User123!`  | USER  |

## Try it

Log in and keep the token:

```bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"Admin123!"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

Get the current user:

```bash
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer $TOKEN"
```

Without a valid token, `/api/auth/me` returns 401.

## Employee API

Every endpoint needs a token from `/api/auth/login`. Any logged-in user can read and export;
only `ADMIN` can change data.

| Method + path                | Role               | Success                             | Errors                                                |
|------------------------------|--------------------|-------------------------------------|-------------------------------------------------------|
| `GET /api/employees`         | any logged-in user | 200 `PageResponse<EmployeeResponse>` | 400, 401                                              |
| `GET /api/employees/{id}`    | any logged-in user | 200 `EmployeeResponse`              | 401, 404                                              |
| `POST /api/employees`        | ADMIN              | 201 + `Location` header             | 400, 401, 403, 409 duplicate email                    |
| `PUT /api/employees/{id}`    | ADMIN              | 200 `EmployeeResponse`              | 400, 401, 403, 404, 409 duplicate email or stale version |
| `DELETE /api/employees/{id}` | ADMIN              | 204                                 | 401, 403, 404                                         |
| `GET /api/employees/export`  | any logged-in user | 200 file download                   | 400, 401                                              |

List and export query parameters:

| Parameter    | Meaning                                                                                                                  |
|--------------|--------------------------------------------------------------------------------------------------------------------------|
| `search`     | Case-insensitive "contains" match on first name, last name, email and job title                                          |
| `department` | Case-insensitive exact match                                                                                             |
| `status`     | `ACTIVE`, `ON_LEAVE` or `TERMINATED`                                                                                     |
| `sort`       | `field,asc` or `field,desc`; default `lastName,asc`. Fields: `firstName`, `lastName`, `email`, `department`, `jobTitle`, `salary`, `hireDate`, `status` |
| `page`       | List only. From 0, default 0                                                                                             |
| `size`       | List only. 1 to 100, default 20                                                                                          |
| `format`     | Export only, required: `csv` or `json`. Export returns at most 10,000 rows                                               |

Emails are stored lowercase, and two employees can't share an email in any case. A `PUT` must
send the `version` it last read; if someone else changed the record since, the API returns 409
and the client should reload it.

### Examples

These use the admin `TOKEN` from [Try it](#try-it).

Create an employee. The response is 201 with a `Location` header, and the body includes the new
`id` and `version`:

```bash
RESPONSE=$(curl -s -i http://localhost:8080/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada.lovelace@example.com",
       "department":"Engineering","jobTitle":"Software Engineer","salary":120000,
       "hireDate":"2024-03-01","status":"ACTIVE"}')
echo "$RESPONSE"
ID=$(echo "$RESPONSE" | sed -nE 's/.*"id":([0-9]+).*/\1/p')
```

List active engineers whose name, email or job title contains "an", highest salary first:

```bash
curl -s 'http://localhost:8080/api/employees?search=an&department=engineering&status=ACTIVE&sort=salary,desc&page=0&size=5' \
  -H "Authorization: Bearer $TOKEN"
```

Update the employee, sending the `version` from the create response (0). The response carries
`version` 1; sending the same request again returns 409 because version 0 is now stale:

```bash
curl -s -X PUT http://localhost:8080/api/employees/$ID \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada.lovelace@example.com",
       "department":"Engineering","jobTitle":"Engineering Manager","salary":135000,
       "hireDate":"2024-03-01","status":"ACTIVE","version":0}'
```

Export the engineers to files, newest hires first:

```bash
curl -s -o employees.csv -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/employees/export?format=csv&department=engineering&sort=hireDate,desc'
curl -s -o employees.json -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/api/employees/export?format=json&department=engineering&sort=hireDate,desc'
```

Delete the employee (204):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE http://localhost:8080/api/employees/$ID \
  -H "Authorization: Bearer $TOKEN"
```

## Errors

Every error is an [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) Problem Details body with
content type `application/problem+json`. `type` is left out when it is the default
`about:blank`. Validation errors add `fieldErrors`:

```json
{
  "detail": "2 fields are invalid",
  "instance": "/api/employees",
  "status": 400,
  "title": "Validation failed",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" },
    { "field": "salary", "message": "must be greater than 0" }
  ]
}
```

| Status | When                                                                                    |
|--------|-----------------------------------------------------------------------------------------|
| 400    | Invalid body, malformed JSON, bad `page`/`size`/`sort`/`format`/`status`, missing `version` on update |
| 401    | Missing, invalid or expired token                                                       |
| 403    | A `USER` tried to create, update or delete                                              |
| 404    | No employee with that id                                                                |
| 409    | Duplicate email, or the record changed since the client read it                         |
| 500    | Unexpected error; details are logged, never returned                                    |

## Tests

Docker must be running: the integration tests start their own PostgreSQL with Testcontainers,
so the docker compose database is not needed.

```bash
cd backend
./mvnw test
```

On Windows without bash, run `mvnw.cmd test`.
