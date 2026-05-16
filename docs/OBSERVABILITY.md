# Observability Guide (Phase 3)

## Request tracing

Every HTTP response includes:

- `X-Correlation-Id` — request correlation (phase 2)
- `X-Trace-Id` / `X-Span-Id` / `traceparent` — distributed trace (Bonus 1)

Send your own values to trace a request through logs and async jobs:

```http
GET /projects/{id}
Authorization: Bearer <token>
X-Correlation-Id: demo-trace-001
```

Log pattern (see `application.yml`):

```text
cid=<correlationId> tenant=<tenantId> job=<jobId>
```

Business events also log under logger `com.workhub.business` with structured key=value pairs.

## Health and readiness

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health/liveness` | Process is up |
| `GET /actuator/health/readiness` | DB + RabbitMQ queue depths ready |
| `GET /actuator/health` | Full health (details when authorized) |

Readiness includes custom indicator `rabbitQueues` with `jobsQueueDepth` and `deadLetterQueueDepth`.

## Metrics (Micrometer)

Exposed at:

- `GET /actuator/metrics` — index
- `GET /actuator/metrics/{name}` — e.g. `workhub.projects.created`
- `GET /actuator/prometheus` — Prometheus scrape format

| Metric | Description |
|--------|-------------|
| `workhub.jobs.created` | Jobs persisted (tag: `tenant`) |
| `workhub.jobs.completed` | Consumer finished (tags: `tenant`, `result=success\|failure`) |
| `workhub.jobs.publish.failed` | Rabbit publish failures |
| `workhub.jobs.retry` | Admin retry invocations |
| `workhub.projects.created` | Projects created |
| `workhub.projects.create.duration` | Project create timer (p50, p95) |
| `workhub.security.cross_tenant.denied` | Blocked cross-tenant access |
| `workhub.quotas.exceeded` | Quota guardrail triggers |
| `workhub.audit.events` | Audit rows written (tag: `event`) |
| `workhub.saga.completed` | Saga outcomes (tag: `result=success\|compensated`) |

## End-to-end trace example (HTTP → service → DB)

1. **Login**

```http
POST /auth/login
Content-Type: application/json
X-Correlation-Id: e2e-demo-01

{"email":"admin@a.com","password":"x"}
```

Response headers include `X-Correlation-Id: e2e-demo-01`.

2. **Create project**

```http
POST /projects
Authorization: Bearer <token>
X-Correlation-Id: e2e-demo-01
Content-Type: application/json

{"name":"E2E Trace Project"}
```

3. **Expected logs** (abbreviated)

```text
... cid=e2e-demo-01 tenant=tenant-a ... HTTP_REQUEST | request completed | method=POST path=/projects status=201 ...
... cid=e2e-demo-01 tenant=tenant-a ... PROJECT_CREATED | project persisted | projectId=... tenantId=tenant-a ...
... cid=e2e-demo-01 tenant=tenant-a ... AUDIT_RECORDED | audit event persisted | eventType=PROJECT_CREATED ...
```

4. **Verify metrics**

```http
GET /actuator/metrics/workhub.projects.created
```

5. **Verify audit row**

```http
GET /admin/audit
Authorization: Bearer <admin-token>
```

## Sample response headers

```http
HTTP/1.1 201 Created
X-Correlation-Id: e2e-demo-01
Content-Type: application/json
```

## Local dependencies

- **PostgreSQL** — required for persistence and DB health
- **RabbitMQ** — required for job processing and queue health (optional for read-only API demos)
- **Redis** — not required (Caffeine in-process cache)

No extra tracing backend is required for marking; correlation ID + Actuator metrics are sufficient.
