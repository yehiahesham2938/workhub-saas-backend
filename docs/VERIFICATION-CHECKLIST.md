# Phase 3 Final Verification Checklist

Run before submission. Record pass/fail and attach evidence where noted.

## Automated

- [ ] `mvn test` — all tests pass
- [ ] No compile warnings in core packages

## Runtime startup

- [ ] Application starts with PostgreSQL configured
- [ ] `GET /actuator/health/liveness` → UP
- [ ] `GET /actuator/health/readiness` → UP (with RabbitMQ running)

## Authentication

- [ ] `POST /auth/login` returns token + tenantId
- [ ] `GET /auth/me` with Bearer token returns same tenant
- [ ] Missing token → 401

## Tenant isolation

- [ ] Tenant A creates project; Tenant B GET by ID → 404
- [ ] Audit shows `CROSS_TENANT_DENIED` for tenant B
- [ ] Metric `workhub.security.cross_tenant.denied` increments

## Phase 3 features

- [ ] Project create writes audit + metric
- [ ] Duplicate job idempotency key returns same job ID
- [ ] Saga success → `COMPLETED` + project exists
- [ ] Saga failure → `COMPENSATED` + no orphan project
- [ ] Workspace over quota → 429
- [ ] Project GET populates cache (second read fast / cache key present)
- [ ] `GET /admin/quotas` shows plan and usage
- [ ] `GET /admin/tenant/summary` shows counts
- [ ] `GET /admin/queues/dead-letter` returns queue names
- [ ] `X-Correlation-Id` echoed on responses

## Failure paths

- [ ] Invalid JWT → 401
- [ ] Member on admin endpoint → 403
- [ ] Tx demo rollback → project not listed

## Documentation

- [ ] README phase 3 section matches endpoints
- [ ] Postman collection includes new admin routes
- [ ] `docs/PHASE3-PROOF.md` steps reproduce locally

## Submission package

- [ ] Branch `phase-3-enterprise` committed
- [ ] Phase 3 guide / proof docs included
- [ ] Demo script or Postman updated
- [ ] Sample logs or screenshots attached per rubric
