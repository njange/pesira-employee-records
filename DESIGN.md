# Design notes

Decisions, assumptions and trade-offs behind [README.md](README.md). The full visual board is in
[docs/design/board.png](docs/design/board.png) and a
[read-only Excalidraw snapshot](https://excalidraw.com/#json=bWrBKrJXH0lBJPsB4Oy6i,IL5EyamP9s5TSoaJvxluIg).

## Scope and assumptions

An internal back-office tool: a few dozen staff and about 10,000 employee records.
Correctness and security matter more than scale. Each answer below is an assumption.

| Question                  | Assumption                                                        |
|---------------------------|-------------------------------------------------------------------|
| Which records?            | Name, email, department, job title, salary, hire date, status     |
| What can each role do?    | Admin: full create, read, update, delete. User: read and export   |
| Can people sign up?       | No. Accounts are seeded by a Flyway migration                     |
| How much data?            | Up to about 10,000 records and 50 users, about 10 at once         |
| Hard or soft delete?      | Hard delete, behind a confirm dialog                              |
| What does export include? | The current search, filter and sort; all pages, up to 10,000 rows |
| Two admins edit one row?  | Optimistic locking: the second save gets a 409                    |

Out of scope: registration, password reset, refresh tokens, audit history, CSV import.

## Trade-offs

Each choice favours simplicity at this size and has a clear point to revisit.

| Choice                          | Instead of                | Why                                              | Revisit when                       |
|---------------------------------|---------------------------|--------------------------------------------------|------------------------------------|
| One Spring Boot app             | Microservices             | One deploy, one transaction, easy to trace       | Separate teams own separate parts  |
| Server-side table               | Loading all rows          | About 5 KB per page at any table size            | Never at this scale                |
| Offset paging                   | Keyset (cursor) paging    | Jump to any page; simple to build                | Past about 100,000 rows            |
| Stateless JWT, 60-minute expiry | Server sessions           | No session store; any instance serves any call   | Instant logout is needed           |
| Spring OAuth2 resource server   | A hand-written JWT filter | Well tested; less custom security code           | Never for this app                 |
| Token in `sessionStorage`       | `httpOnly` cookie         | No CSRF handling needed; closes with the tab     | Before handling sensitive data     |
| Optimistic locking (`@Version`) | Row locks                 | No locks held while someone edits                | Edits collide often                |
| Hard delete                     | Soft delete               | Matches the brief; simpler queries               | Undo or audit is needed            |
| `ILIKE` search                  | `pg_trgm` or full-text    | No extension; a full scan of 10,000 rows is fast | Past about 100,000 rows            |
| Flyway SQL migrations           | Liquibase                 | Plain SQL a reviewer can read                    | Several database vendors           |
| Department as text              | A table and foreign key   | Simple form and seed data                        | Departments get their own data     |
| Next.js `/api` proxy            | Calling the API directly  | The browser talks to one origin                  | The API is public to other clients |

The proxy still passes the browser's `Origin` header, so the backend keeps a CORS list.
`CORS_ALLOWED_ORIGINS` accepts patterns, so Vercel preview URLs work without a code change.

## Request rules

- Every API call except login passes the JWT check first; `@PreAuthorize` repeats the Admin check on writes.
- Controllers validate DTOs; services trim strings, lowercase emails and run in a transaction.
- The sort field comes from a whitelist, and `id` is always added as a tie-breaker so pages stay stable.
- A page past the end returns 200 with empty `content` and the real totals.
- One `GlobalExceptionHandler` turns every error into Problem Details; a 500 never includes a stack trace.

## Failure cases

| Case                          | What happens                                                              |
|-------------------------------|---------------------------------------------------------------------------|
| Two admins edit one row       | The second save gets 409; the form offers "Reload record"                 |
| Two requests create one email | The unique index rejects the second; the API returns 409                  |
| Token expires mid-edit        | The next call gets 401; the frontend clears the session and shows login   |
| Database is down              | Calls fail with a Problem Details error; transactions roll back           |
| Very large export             | Streamed in 500-row pages and capped at 10,000 rows, so memory stays flat |
| CSV opened in a spreadsheet   | Text starting with `=`, `+`, `-`, `@`, tab or CR gets a leading `'`       |

The frontend tells a duplicate email from a stale version after a 409 on edit by re-reading the
record: a newer `version` means stale. The API gives both the same plain 409.

## How it would scale

| Size                         | Change                                                                                                                  |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Today: 10,000 rows, 50 users | One API instance, one PostgreSQL, offset paging, `ILIKE`                                                                |
| 10 times that                | Two or three API instances behind a load balancer (JWT is stateless), keyset paging, a `pg_trgm` index                  |
| 100 times that               | A read replica for lists and exports, exports as background jobs, refresh tokens with revocation, an audit log, metrics |
