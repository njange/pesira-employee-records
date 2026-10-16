# Pesira Employee Records

A Spring Boot and PostgreSQL API, with JWT authentication, for managing employee records.

## Prerequisites

- JDK 21 or newer
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
