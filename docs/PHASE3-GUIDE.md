# Phase 3 Complete Guide (Submission)

This guide describes the **current** `workhub-saas-backend` repository on branch `phase-3-enterprise`. It assumes phase 2 (auth, tenant isolation, CRUD, jobs, actuator) is already in place and documents only what phase 3 adds.

## 1. Baseline (phase 2 — already present)

Do not re-implement:

- JWT auth, RBAC, `401`/`403`
- `tenant_id` on entities and service checks
- Projects, tasks, workspaces, async jobs
- Correlation ID filter, basic Actuator health/metrics
- Integration tests for isolation and RBAC

## 2. Core phase 3 features (required)

| Step | Feature | Verification |
|------|---------|--------------|
| 2 | Audit trail | `GET /admin/audit` after `POST /projects` |
| 3 | Idempotent jobs | Same `idempotencyKey` → same job ID |
| 4 | Saga compensation | `POST /projects/provision-saga` + `simulateFailure` |
| 5 | Tenant quotas | Second workspace on STARTER (test limits) → 429 |
| 6 | Caching | Repeated `GET /projects/{id}` uses cache |
| 7 | Observability | Metrics + `X-Correlation-Id` + business logs |
| 8 | Admin APIs | Quotas, summary, DLQ inspect, job retry |
| 9 | Tests | `mvn test` — `Phase3*` IT classes |
| 10 | Docs | This guide, `PHASE3-PROOF.md`, `OBSERVABILITY.md` |
| 11 | E2E pass | `scripts/phase3-demo.ps1` |
| 12 | Submit | Attach repo + proof docs to Classroom |

Details: [PHASE3-PROOF.md](PHASE3-PROOF.md), [VERIFICATION-CHECKLIST.md](VERIFICATION-CHECKLIST.md).

## 3. Bonus features (optional — clearly marked)

See [BONUS-FEATURES.md](BONUS-FEATURES.md). Summary:

1. Distributed tracing (`X-Trace-Id`, job propagation)
2. Extra caches (`tenantConfig`, `tenantDashboard`)
3. DLQ replay endpoint
4. Export endpoints under `/admin/exports/*`
5. `docker compose up --build`
6. GitHub Actions CI
7. Plan-based feature flags

## 4. Quick start

### Local (Maven)

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/workhub"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
mvn spring-boot:run
```

### Docker (Bonus 5)

```bash
docker compose up --build
```

### Demo script

```powershell
.\scripts\phase3-demo.ps1
```

## 5. Sample request (HTTP → log → DB)

```http
POST /projects
Authorization: Bearer <admin-a-token>
X-Correlation-Id: classroom-demo-01
X-Trace-Id: trace-classroom-01
Content-Type: application/json

{"name":"Submission Demo Project"}
```

**Expected:**

- Response `201` with `X-Correlation-Id` and `X-Trace-Id` headers
- Log lines: `PROJECT_CREATED`, `AUDIT_RECORDED`, `HTTP_REQUEST`
- Row in `audit_events` for `PROJECT_CREATED`
- Metric `workhub.projects.created` incremented

## 6. Final submission checklist

- [ ] Guide matches implementation (this file + `PHASE3-PROOF.md`)
- [ ] Guide does not assume missing phase 2 work
- [ ] Core phase 3 features implemented, tested, documented
- [ ] Bonus features labeled optional in `BONUS-FEATURES.md`
- [ ] `mvn test` passes locally (and CI if using GitHub)
- [ ] Postman: `postman/Workhub-Phase3.postman_collection.json`
- [ ] Final file versions attached to Google Classroom

## 7. Commit history (suggested)

```text
feat(audit): add tenant-aware audit trail for enterprise actions
feat(jobs): harden async processing with idempotent workflow support
feat(workflows): add saga compensation for multi-step enterprise flow
feat(tenancy): enforce tenant quotas and usage guardrails
perf(cache): add tenant-safe caching for hot read paths
feat(observability): expand tracing and metrics for enterprise workflows
feat(admin): add operational endpoints for support and diagnostics
test(enterprise): add phase 3 coverage for audit quotas and sagas
docs: update phase 3 guide and verification evidence
feat(bonus): tracing, exports, dlq replay, docker, ci, feature flags
```
