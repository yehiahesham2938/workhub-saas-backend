## Project Overview

Workhub SaaS Backend is a Spring Boot multi-tenant scaffold following a clean layered architecture. It demonstrates:
- JWT-based stateless authentication
- Strong tenant isolation via `tenant_id` on all domain entities
- Transactional integrity with rollback (project + tasks demo)
- DTOs and validation on all request models
- Phase 3 enterprise features: audit, idempotent jobs, saga compensation, quotas, caching, observability, and admin operations

## Architecture (Layered)
- Controller: HTTP layer, request validation, no business logic
- Service: Business logic, transaction boundaries, tenant checks
- Repository: Spring Data JPA repositories
- Security: JWT filter -> `SecurityContext`; Tenant filter -> `TenantContext`
- Entity: Domain models with `tenant_id` for isolation

## How to Run Locally

Prereqs: Java 17+, Maven, PostgreSQL. RabbitMQ recommended for async jobs.

Environment variables (can be in `.env` due to spring-dotenv):

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/workhub` | Database |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `password` | Database password |
| `JWT_SECRET` | (see `application.yml`) | JWT signing |
| `JWT_EXPIRATION_MS` | `3600000` | Token TTL |
| `RABBITMQ_HOST` | `localhost` | Message broker |
| `SERVER_PORT` | `8080` | HTTP port |

Example (PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/workhub"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="password"
mvn spring-boot:run
```

Phase 3 demo script: `scripts/phase3-demo.ps1`

## API Overview

### Auth
- `POST /auth/login` -> `{ token, userId, tenantId, role }`
  - `admin@a.com` -> `tenant-a`, `admin@b.com` -> `tenant-b`
- `GET /auth/me` -> current user from JWT

### Projects
- `POST /projects` — create project
- `GET /projects`, `GET /projects/{id}` — list / get (cached read)
- `DELETE /projects/{id}` — admin only
- `POST /projects/{id}/tasks` — create task
- `POST /projects/tx-demo` — transactional rollback demo
- `POST /projects/provision-saga` — saga with optional `simulateFailure`

### Jobs
- `POST /jobs` — body optional: `{ "idempotencyKey": "key-1" }`
- `GET /jobs/{id}`

### Admin (admin role, tenant-scoped)
- `GET /admin/audit` — audit log
- `GET /admin/quotas` — plan usage vs limits
- `GET /admin/tenant/summary` — support snapshot
- `GET /admin/queues/dead-letter` — queue depths
- `POST /admin/jobs/{id}/retry` — re-queue FAILED/PENDING job

### Observability (mostly public)
- `GET /actuator/health`, `/actuator/health/readiness`, `/actuator/health/liveness`
- `GET /actuator/metrics`, `/actuator/metrics/{name}`, `/actuator/prometheus`

All protected requests: `Authorization: Bearer <token>`. Optional: `X-Correlation-Id` for log tracing.

## Phase 3 Enterprise Features

| Feature | Details |
|---------|---------|
| Audit trail | Append-only events; see `GET /admin/audit` |
| Idempotent jobs | Same `idempotencyKey` returns same job |
| Saga | `provision-saga` compensates on failure |
| Quotas | Plan limits in `workhub.quotas`; HTTP 429 when exceeded |
| Caching | Caffeine (no Redis required locally) |
| Observability | Correlation ID, Micrometer metrics, structured business logs |
| Admin ops | Quotas, summary, DLQ inspect, job retry |

Documentation:
- [docs/PHASE3-PROOF.md](docs/PHASE3-PROOF.md) — marker verification guide
- [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) — tracing and metrics
- [docs/VERIFICATION-CHECKLIST.md](docs/VERIFICATION-CHECKLIST.md) — pre-submission checklist
- [docs/PHASE3-SCOPE.md](docs/PHASE3-SCOPE.md) — scope and acceptance criteria

Postman: [postman/Workhub-Phase3.postman_collection.json](postman/Workhub-Phase3.postman_collection.json)

## Tenant Isolation
- JWT carries tenant identity; services enforce `tenant_id` on all access
- Cross-tenant access returns 404 and records `CROSS_TENANT_DENIED` audit + metric

## Tests

```powershell
mvn test
```

Key suites: `Phase3EnterpriseIT`, `Phase3ObservabilityIT`, `Phase3AdminOperationsIT`, plus phase 2 isolation/RBAC tests.
