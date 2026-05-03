## Tenant Isolation Policy and Verification

### Step 2.2 - Consistent cross-tenant policy

Policy used by this project:

- Cross-tenant object access returns `404 Not Found` (anti-enumeration).
- `403 Forbidden` is reserved for authorization/role violations where the resource ownership is otherwise valid.
- `401 Unauthorized` is only for authentication failures (missing/invalid auth).

How this is enforced:

- Service-layer object lookups use tenant-scoped repository methods (`findByIdAndTenantId`, `findAllByTenantId`) as the primary isolation mechanism.
- Global exception handling maps:
  - `ResourceNotFoundException` -> `404`
  - `AccessDeniedException` -> `403`
  - `AuthenticationException` -> `401`

### Step 2.3 - Tenant matrix (read/write/list/update/delete)

Prerequisites:

- `TOKEN_A_ADMIN`: tenant A, role ADMIN
- `TOKEN_A_USER`: tenant A, role USER
- `TOKEN_B_ADMIN`: tenant B, role ADMIN
- Base URL: `http://localhost:8080`

Seed tenant A resources:

```bash
# Create project under tenant A
PROJECT_A_JSON=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Authorization: Bearer $TOKEN_A_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"name":"A-Project"}')
PROJECT_A_ID=$(echo "$PROJECT_A_JSON" | jq -r '.id')

# Create task under tenant A project
TASK_A_JSON=$(curl -s -X POST "$BASE_URL/projects/$PROJECT_A_ID/tasks" \
  -H "Authorization: Bearer $TOKEN_A_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"status":"TODO"}')
TASK_A_ID=$(echo "$TASK_A_JSON" | jq -r '.id')

# Create workspace under tenant A
WORKSPACE_A_JSON=$(curl -s -X POST "$BASE_URL/api/v1/workspaces" \
  -H "Authorization: Bearer $TOKEN_A_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"name":"A-Workspace","ownerEmail":"a@ex.com"}')
WORKSPACE_A_ID=$(echo "$WORKSPACE_A_JSON" | jq -r '.id')

# Create job under tenant A
JOB_A_JSON=$(curl -s -X POST "$BASE_URL/jobs" \
  -H "Authorization: Bearer $TOKEN_A_ADMIN")
JOB_A_ID=$(echo "$JOB_A_JSON" | jq -r '.id')
```

Tenant B cross-tenant attempts against tenant A resources:

```bash
# 1) /projects list (B should not see A project)
curl -s -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/projects"
# Expected: 200 with no PROJECT_A_ID in response content

# 2) /projects/{id} get
curl -i -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/projects/$PROJECT_A_ID"
# Expected: 404

# 3) /projects/{id} delete
curl -i -X DELETE -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/projects/$PROJECT_A_ID"
# Expected: 404

# 4) /projects/{id}/tasks nested create under A parent
curl -i -X POST "$BASE_URL/projects/$PROJECT_A_ID/tasks" \
  -H "Authorization: Bearer $TOKEN_B_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
# Expected: 404

# 5) /tasks/{id} update
curl -i -X PATCH "$BASE_URL/tasks/$TASK_A_ID" \
  -H "Authorization: Bearer $TOKEN_B_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"status":"DONE"}'
# Expected: 404

# 6) /api/v1/workspaces list (B should not see A workspace)
curl -s -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/api/v1/workspaces"
# Expected: 200 with no WORKSPACE_A_ID in response content

# 7) /api/v1/workspaces/{id} get
curl -i -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/api/v1/workspaces/$WORKSPACE_A_ID"
# Expected: 404

# 8) /jobs/{id} read isolation
curl -i -H "Authorization: Bearer $TOKEN_B_ADMIN" "$BASE_URL/jobs/$JOB_A_ID"
# Expected: 404
```

Role-based (same tenant) verification for `403` policy:

```bash
# Same-tenant USER attempts admin-only delete
curl -i -X DELETE -H "Authorization: Bearer $TOKEN_A_USER" "$BASE_URL/projects/$PROJECT_A_ID"
# Expected: 403 (role violation)
```

Expected matrix summary:

- Cross-tenant list operations: `200` with tenant-filtered empty/own-only data.
- Cross-tenant get/update/delete/create-child-by-foreign-parent: `404`.
- Same-tenant role violation: `403`.
- Missing/invalid token: `401`.

### Step 2.4 - Automated tests backing this proof

Run:

```bash
mvn -Dtest='TenantIsolation*IT' test
```

Tests in [src/test/java/com/workhub/saasbackend/integration/](src/test/java/com/workhub/saasbackend/integration/):

- `TenantIsolationProjectIT` - cross-tenant read/list/delete/nested-create
- `TenantIsolationTaskIT` - cross-tenant update via `PATCH /tasks/{id}`
- `TenantIsolationWorkspaceIT` - workspace list/get isolation
- `TenantIsolationJobIT` - job read isolation
- `AbstractTenantIsolationIT` - shared seed / token harness

Each test seeds tenant B data, calls every endpoint with tenant A's JWT, and asserts the documented status codes.

### Step 2.5 - End-to-end Phase 2 demo

For a one-shot, scripted, end-to-end verification (login, async job, cross-tenant denial, observability):

```bash
BASE_URL=http://localhost:8080 ./scripts/phase2-demo.sh
```

The script exits non-zero if any expected status code does not match.

A matching Postman collection covering the same flows (auth, async job, cross-tenant, RBAC matrix, observability) is at [postman/Workhub-Phase2.postman_collection.json](postman/Workhub-Phase2.postman_collection.json). It can be run interactively in Postman or headlessly via Newman:

```bash
newman run postman/Workhub-Phase2.postman_collection.json --env-var baseUrl=http://localhost:8080
```

