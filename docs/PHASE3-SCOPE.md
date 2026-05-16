# Phase 3 Scope

## Baseline

Phase 3 extends the phase 2 foundation on branch `phase-3-enterprise`. No rewrites of auth, tenant isolation, or core CRUD.

## Features In Scope

| Feature | Acceptance criteria |
|---------|---------------------|
| Audit trail | Business events persisted with tenant, actor, resource; admin read API; cross-tenant and auth denials logged |
| Idempotent jobs | Optional idempotency key; duplicate submit returns same job; consumer skips terminal states |
| Saga compensation | Project + default tasks provisioning with step tracking and rollback on failure |
| Tenant quotas | Plan-based limits on workspaces, projects, open jobs, tasks per project; HTTP 429 on exceed |
| Tenant-safe cache | Caffeine cache on hot reads; tenant-scoped keys; eviction on writes |
| Observability | Correlation ID, Micrometer metrics, structured logs, queue health |
| Admin operations | Audit, quotas, tenant summary, DLQ inspect, job retry |

## Verification plan

1. `mvn test` — all phase 2 + phase 3 tests pass
2. Create project → audit record exists
3. Duplicate job with same idempotency key → single job
4. Saga failure path → partial resources compensated
5. Exceed workspace quota → 429
6. Repeated project GET → cache hit (log/metrics optional)

## Out of scope

- Redis (optional; Caffeine used for local/demo)
- UI changes
- Billing integration
