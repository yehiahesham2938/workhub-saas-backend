## Project Overview

Workhub SaaS Backend is a Spring Boot multi-tenant scaffold following a clean layered architecture. It demonstrates:
- JWT-based stateless authentication
- Strong tenant isolation via `tenant_id` on all domain entities
- Transactional integrity with rollback (project + tasks demo)
- DTOs and validation on all request models

## Architecture (Layered)
- Controller: HTTP layer, request validation, no business logic
- Service: Business logic, transaction boundaries, tenant checks
- Repository: Spring Data JPA repositories
- Security: JWT filter -> `SecurityContext`; Tenant filter -> `TenantContext`
- Entity: Domain models with `tenant_id` for isolation

Folders:
```
src/main/java/com/workhub/saasbackend
├── config
├── controller
├── dto
│   ├── request
│   ├── response
│   └── shared
├── entity
├── exception
├── repository
├── security
└── service
    └── impl
```

## How to Run Locally
Prereqs: Java 21, Maven, PostgreSQL running locally.

Environment variables (can be in `.env` due to spring-dotenv):
- DB_URL=jdbc:postgresql://localhost:5432/workhub
- DB_USERNAME=postgres
- DB_PASSWORD=postgres
- JWT_SECRET=base64-encoded-256bit-secret
- JWT_EXPIRATION_MS=3600000

Example (PowerShell):
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/workhub"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="password"
$env:JWT_SECRET="VGhpcy1pcy1ub3QtMy1yZWFsbHktYS0yNTYtYml0LXNlY3JldC1zdHJpbmchISE="
$env:JWT_EXPIRATION_MS="3600000"
mvn spring-boot:run
```

## API Overview
Auth:
- POST `/auth/login` -> returns `{ token, userId, tenantId, role }`
  - Sample body: `{"email":"admin@a.com","password":"Password123!"}`
  - Tenant is derived from email domain for demo: `a.com -> tenant-a`, `b.com -> tenant-b`
- GET `/auth/me` -> returns current `{ userId, tenantId, role }` from JWT

Projects:
- POST `/projects` -> create project `{ "name": "Project X" }`
- GET `/projects` -> list paged projects
- GET `/projects/{id}` -> get project
- DELETE `/projects/{id}` -> admin only
- POST `/projects/{id}/tasks` -> create task for project `{ "status": "TODO|IN_PROGRESS|DONE" }`

Transactional Demo (Rollback):
- POST `/projects/tx-demo` with body:
  ```
  {
    "name": "TX Demo Project",
    "taskStatuses": ["TODO", "IN_PROGRESS", "DONE"]
  }
  ```
  Behavior: The service creates the project and the first task, then throws an unchecked exception. Because the method is annotated with `@Transactional`, the entire operation rolls back. No project or tasks should remain in the database.

Verification steps:
1) Call `/auth/login` to get `token`
2) Call `/auth/me` with `Authorization: Bearer <token>` and confirm `tenantId`
3) Call `/projects/tx-demo` with the token
4) Call `/projects` and confirm that the created project is NOT present (rollback worked)

Headers:
- `Authorization: Bearer <token>` (required for all protected endpoints)

Postman:
- See `postman/Workhub.postman_collection.json`
- Update `{{baseUrl}}` and set `{{jwt}}` from `/auth/login` response

## Tenant Isolation
- `TenantFilter` sets `TenantContext` from JWT claims
- Every service checks tenant equality before access/mutation
- Repositories query per-tenant where needed
- Prevents cross-tenant leaks

## Transaction Boundary
- `@Transactional` at service layer
- Demo method: `ProjectService.createProjectWithTasksAndRollback` creates project+tasks then throws an exception to verify rollback

## DTOs + Validation
- All requests validated with `jakarta.validation` annotations
- Clear separation of request/response models

## Observability
- Basic correlation-id filter for tracing
