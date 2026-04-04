---
alwaysApply: false
---
## How to Test Tenant Isolation

- Prereqs: Two JWTs with different tenantId claims: `tenantA`, `tenantB`.
- Use distinct Authorization headers for each.

### Create resources under tenantA
```bash
curl -s -X POST http://localhost:8080/api/v1/workspaces \
  -H "Authorization: Bearer <token-tenantA>" -H "Content-Type: application/json" \
  -d '{"name":"A-Workspace","ownerEmail":"a@ex.com"}'
```
```bash
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer <token-tenantA>" -H "Content-Type: application/json" \
  -d '{"name":"A-Project"}'
```

### Verify tenantA can list/find only its data
```bash
curl -s -H "Authorization: Bearer <token-tenantA>" http://localhost:8080/projects
```
Expected: Only projects created by `tenantA`.

### Cross-tenant access must fail
- Try to GET a `tenantA` project with `tenantB` token:
```bash
curl -i -H "Authorization: Bearer <token-tenantB>" http://localhost:8080/projects/<tenantA-project-id>
```
Expected: 403 Forbidden (Access denied: tenant mismatch).

### Updates are tenant-scoped
- Create Task under a Project with same-tenant token works.
- Using other-tenant token returns 404 for update attempts.

### Deletion restricted by tenant and role
- ADMIN from same tenant can DELETE its project:
```bash
curl -i -X DELETE -H "Authorization: Bearer <admin-token-tenantA>" http://localhost:8080/projects/<id>
```
Expected: 204 No Content.
- Other-tenant token: 403 Forbidden (tenant mismatch).
- USER role in same tenant: 403 Forbidden (role restriction).

Notes:
- All repositories use `findByIdAndTenantId` and list by `tenantId`.
- Services pull tenant via `TenantContext` in every operation.

