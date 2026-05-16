# Phase 3 Final Verification Checklist

Run before submission. Record pass/fail and attach evidence where noted.

**Automated coverage (integration/unit tests on `main`):** `Phase3EnterpriseIT`, `Phase3ObservabilityIT`, `Phase3AdminOperationsIT`, `Phase3BonusIT`, `JobConsumerTest`, `BusinessMetricsTest`, plus phase 2 isolation/RBAC suites.

## Automated

- [x] `mvn test` — run locally or via `.github/workflows/ci.yml` (items below covered when green)
- [x] No compile warnings in core packages (CI `mvn verify`)

## Runtime startup

- [ ] Application starts with PostgreSQL configured
- [ ] `GET /actuator/health/liveness` → UP
- [ ] `GET /actuator/health/readiness` → UP (with RabbitMQ running)

## Authentication

- [ ] `POST /auth/login` returns token + tenantId
- [ ] `GET /auth/me` with Bearer token returns same tenant
- [ ] Missing token → 401

## Tenant isolation

- [x] Tenant A creates project; Tenant B GET by ID → 404 (`TenantIsolationProjectIT`, `SecurityResponseBehaviorIT`)
- [x] Audit shows `CROSS_TENANT_DENIED` for tenant B (`Phase3EnterpriseIT`)
- [x] Metric `workhub.security.cross_tenant.denied` increments (`Phase3ObservabilityIT`)

## Phase 3 features

- [x] Project create writes audit + metric (`Phase3EnterpriseIT`, `Phase3ObservabilityIT`)
- [x] Job create writes `JOB_CREATED` audit (`Phase3EnterpriseIT`)
- [x] Job consumer lifecycle audits (`JobConsumerTest`)
- [x] Duplicate job idempotency key returns same job ID (`Phase3EnterpriseIT`)
- [x] Saga success → `COMPLETED` + project exists (`Phase3EnterpriseIT`)
- [x] Saga failure → `COMPENSATED` + no orphan project (`Phase3EnterpriseIT`)
- [x] Workspace over quota → 429 (`Phase3EnterpriseIT`)
- [x] Project GET populates cache (`Phase3EnterpriseIT`)
- [x] `GET /admin/quotas` shows plan and usage (`Phase3AdminOperationsIT`)
- [x] `GET /admin/tenant/summary` shows counts (`Phase3AdminOperationsIT`)
- [x] `GET /admin/queues/dead-letter` returns queue names (`Phase3AdminOperationsIT`)
- [x] `X-Correlation-Id` echoed on responses (`Phase3ObservabilityIT`)

## Failure paths

- [x] Missing token → 401 (`SecurityResponseBehaviorIT`)
- [x] Member on admin endpoint → 403 (`Phase3AdminOperationsIT`, `SecurityResponseBehaviorIT`)
- [ ] Tx demo rollback → project not listed (manual: `DELETE /projects/{id}/tx-demo`)

## Documentation

- [x] README phase 3 section matches endpoints
- [x] Postman collection includes new admin routes (`postman/Workhub-Phase3.postman_collection.json`)
- [ ] `docs/PHASE3-PROOF.md` steps reproduce locally (run `scripts/phase3-demo.ps1` with app up)

## Bonus features (optional)

- [x] `X-Trace-Id` returned on HTTP responses (`Phase3BonusIT`)
- [x] `GET /api/v1/tenant/config` shows plan + features (cached) (`Phase3BonusIT`)
- [x] `GET /admin/exports/audit` works on STARTER (tenant-a) (`Phase3BonusIT`)
- [x] `POST /admin/queues/dead-letter/replay` forbidden on STARTER, OK on PRO (tenant-b) (`Phase3BonusIT`)
- [ ] `docker compose up --build` starts stack (manual)
- [ ] CI workflow passes on GitHub (confirm in Actions after push)

## Final submission (Google Classroom)

- [x] Phase 3 guide matches implementation (`docs/PHASE3-GUIDE.md`)
- [x] Guide assumes phase 2 baseline only (no missing scaffold)
- [x] Core features implemented, tested, documented
- [x] Bonus features clearly marked optional (`docs/BONUS-FEATURES.md`)
- [ ] Final deliverables attached; **final version** of every file submitted (you)

## Submission package

- [x] Branch `phase-3-enterprise` committed (merged to `main` at same SHA)
- [x] Phase 3 guide / proof docs included
- [x] Demo script or Postman updated
- [ ] Sample logs or screenshots attached per rubric (you)
