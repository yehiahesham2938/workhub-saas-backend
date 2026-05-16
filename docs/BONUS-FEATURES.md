# Phase 3 Bonus Features (Optional)

All items below are **optional extras** beyond the core phase 3 requirements. They are implemented on branch `phase-3-enterprise` and marked clearly for markers.

## Bonus 1 — Distributed tracing

- `DistributedTraceFilter` accepts `traceparent` (W3C) or `X-Trace-Id` / `X-Span-Id`
- Response echoes `X-Trace-Id`, `X-Span-Id`, `traceparent`
- `JobMessage` carries `traceId` and `spanId`; consumer restores MDC for async continuity
- Logs include `trace=` and `span=` (see `application.yml` logging pattern)

**Demo:** send `X-Trace-Id: demo-trace-001` on `POST /projects`, then grep logs for `trace=demo-trace-001`.

## Bonus 2 — Extended tenant-safe caching

| Cache | Key pattern | Content |
|-------|-------------|---------|
| `tenantConfig` | `{tenantId}` | Plan + enabled features |
| `tenantDashboard` | `{tenantId}` | Admin tenant summary |
| `projects` | `{tenantId}:{id}` | Project reads |
| `workspaces` | `{tenantId}:...` | Workspace reads |

`GET /api/v1/tenant/config` returns cached tenant metadata.

## Bonus 3 — DLQ replay (admin recovery)

- `POST /admin/queues/dead-letter/replay?limit=10`
- **Admin + PRO plan** (`DLQ_REPLAY` feature)
- Only messages for **current tenant**; foreign-tenant messages returned to DLQ
- Skips jobs already `DONE` (no duplicate side effects)
- Full audit trail (`ADMIN_ACTION`)

## Bonus 4 — Export / reporting

Tenant-scoped, paginated (max 100/page), requires `EXPORT_REPORTS` (STARTER+):

| Endpoint | Output |
|----------|--------|
| `GET /admin/exports/audit` | Audit events |
| `GET /admin/exports/jobs` | Job history |
| `GET /admin/exports/projects` | Project list |
| `GET /admin/exports/usage` | Usage summary |

## Bonus 5 — Docker / compose

```bash
docker compose up --build
```

Services: PostgreSQL, RabbitMQ (management UI on 15672), application on 8080.

## Bonus 6 — CI

GitHub Actions workflow `.github/workflows/ci.yml`: compile, test, package on push/PR.

## Bonus 7 — Feature flags by plan

| Plan | Features |
|------|----------|
| FREE | (none) |
| STARTER | `EXPORT_REPORTS`, `SAGA_PROVISIONING` |
| PRO | + `DLQ_REPLAY`, `ADVANCED_OBSERVABILITY` |
| ENTERPRISE | All features |

Disabled features return **403** with clear message. Inspect flags via `GET /api/v1/tenant/config`.
