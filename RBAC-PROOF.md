## RBAC Status Matrix

Role naming convention:

- JWT roles map to authorities in filter logic:
  - `ADMIN` -> `ROLE_ADMIN`
  - `MEMBER` -> `ROLE_USER`
  - `VIEWER` -> `ROLE_VIEWER`
- Method security uses explicit authority checks (`hasAuthority` / `hasAnyAuthority`).

Expected HTTP status codes per protected endpoint:

| Endpoint | Method | No token | Member (`ROLE_USER`) | Admin (`ROLE_ADMIN`) |
|---|---|---:|---:|---:|
| `/auth/me` | GET | 401 | 200 | 200 |
| `/projects` | POST | 401 | 201 | 201 |
| `/projects` | GET | 401 | 200 | 200 |
| `/projects/{id}` | GET | 401 | 200 | 200 |
| `/projects/{id}/tasks` | POST | 401 | 201 | 201 |
| `/projects/{id}` | DELETE | 401 | 403 | 204 |
| `/projects/tx-demo` | POST | 401 | 202 | 202 |
| `/tasks/{id}` | PATCH | 401 | 200 | 200 |
| `/api/v1/workspaces` | POST | 401 | 201 | 201 |
| `/api/v1/workspaces` | GET | 401 | 200 | 200 |
| `/api/v1/workspaces/{id}` | GET | 401 | 200 | 200 |
| `/jobs` | POST | 401 | 202 | 202 |
| `/jobs/{id}` | GET | 401 | 200 | 200 |

Automated verification:

- Integration test: `RBACMatrixIT`
- Test strategy:
  - Seed same-tenant resources with admin token.
  - Execute each protected endpoint with:
    - no token
    - member token
    - admin token
  - Assert status codes match the matrix above.
