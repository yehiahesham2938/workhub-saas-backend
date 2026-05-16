# Phase 3 Proof Document

Plain-language summary for markers: what was added, how to verify it, and what evidence to capture.

## Feature summary

| Area | What it does |
|------|----------------|
| Audit trail | Append-only `audit_events`; admin list at `GET /admin/audit` |
| Idempotent jobs | `idempotencyKey` on `POST /jobs`; consumer skips terminal states |
| Saga | `POST /projects/provision-saga` with compensation on failure |
| Quotas | Plan limits; HTTP 429 when exceeded |
| Caching | Caffeine, tenant-scoped keys on project/workspace reads |
| Observability | Correlation ID, business metrics, structured logs, queue health |
| Admin ops | Quota usage, tenant summary, DLQ inspect, job retry |

## Admin / operational endpoints

All require `ROLE_ADMIN` and use JWT tenant context.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/admin/audit` | Paginated audit log for current tenant |
| GET | `/admin/quotas` | Plan + usage vs limits |
| GET | `/admin/tenant/summary` | Resource counts for support |
| GET | `/admin/queues/dead-letter` | Jobs queue + DLQ depths |
| POST | `/admin/jobs/{id}/retry` | Re-queue FAILED/PENDING job |

## Verification commands

```powershell
# 1. Tests
mvn test

# 2. Run app
mvn spring-boot:run

# 3. Login (tenant-a admin)
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/login `
  -ContentType application/json `
  -Headers @{ "X-Correlation-Id" = "demo-01" } `
  -Body '{"email":"admin@a.com","password":"x"}'
$headers = @{ Authorization = "Bearer $($login.token)"; "X-Correlation-Id" = "demo-01" }

# 4. Create project + check metrics
Invoke-RestMethod -Method Post -Uri http://localhost:8080/projects -Headers $headers `
  -ContentType application/json -Body '{"name":"Proof Project"}'
Invoke-RestMethod -Uri http://localhost:8080/actuator/metrics/workhub.projects.created

# 5. Audit
Invoke-RestMethod -Uri http://localhost:8080/admin/audit -Headers $headers

# 6. Quotas
Invoke-RestMethod -Uri http://localhost:8080/admin/quotas -Headers $headers

# 7. Saga failure
Invoke-RestMethod -Method Post -Uri http://localhost:8080/projects/provision-saga -Headers $headers `
  -ContentType application/json `
  -Body '{"name":"SagaFail","defaultTaskStatuses":["TODO","IN_PROGRESS"],"simulateFailure":true}'
```

Or run: `scripts/phase3-demo.ps1`

## Expected HTTP status codes

| Scenario | Code |
|----------|------|
| Valid create | 201 / 202 |
| Cross-tenant access | 404 (no data leak) |
| Quota exceeded | 429 |
| Missing/invalid JWT | 401 |
| Wrong role | 403 |
| Saga compensated | 200 + `status: COMPENSATED` |

## Test coverage map

| Test class | Proves |
|------------|--------|
| `Phase3EnterpriseIT` | Audit, idempotency, saga, quota, cache, cross-tenant |
| `Phase3ObservabilityIT` | Correlation header, metrics, actuator |
| `Phase3AdminOperationsIT` | Admin endpoints, tenant isolation |
| `BusinessMetricsTest` | Micrometer counters |
| Phase 2 ITs | RBAC, tenant isolation (unchanged) |

## Screenshots / logs to capture for submission

1. `mvn test` summary (all green)
2. Actuator metrics snippet for `workhub.projects.created`
3. Log lines showing `cid=` and `PROJECT_CREATED`
4. `GET /admin/audit` JSON excerpt
5. Cross-tenant 404 + `CROSS_TENANT_DENIED` audit row
6. Saga `COMPENSATED` response body

See also: `docs/OBSERVABILITY.md`, `docs/PHASE3-SCOPE.md`, `docs/VERIFICATION-CHECKLIST.md`.
