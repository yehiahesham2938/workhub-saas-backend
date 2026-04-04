---
alwaysApply: false
---
## Demo Script

### 1) Login (example - token issuance depends on your auth flow)
Assume you have a valid JWT with claims: `sub` (userId UUID), `tenantId`, `role`.

### 2) Create Job
```bash
curl -s -X POST http://localhost:8080/jobs \
  -H "Authorization: Bearer <token>" \
  -H "X-Correlation-Id: demo-run-1"
```
Response:
```json
{"id":"<jobId>","status":"PENDING", "...": "..."}
```

### 3) Check Job Status
Immediately:
```bash
curl -s http://localhost:8080/jobs/<jobId> -H "Authorization: Bearer <token>"
```
Expected: status PENDING or PROCESSING.

After ~2 seconds:
```bash
curl -s http://localhost:8080/jobs/<jobId> -H "Authorization: Bearer <token>"
```
Expected: status DONE (or FAILED if simulated error).

### 4) RBAC Quick Check
- Create project (allowed for ADMIN/MEMBER/VIEWER):
```bash
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer <member-or-admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Project"}'
```
- Delete project (ADMIN only):
```bash
curl -i -X DELETE http://localhost:8080/projects/<id> \
  -H "Authorization: Bearer <admin-token>"
```
Member token should return `403 Forbidden`.

