# workhub-saas-backend

Spring Boot multi-tenant SaaS backend scaffold using clean layered architecture.

## Tech Stack

- Java 17+
- Maven
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Lombok
- Validation

## Package Structure

```
src/main/java/com/workhub/saasbackend
├── config
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── exception
├── repository
├── security
└── service
		└── impl
```

## Multi-Tenancy Model

- Shared database, shared schema, tenant isolation by `tenant_id` column.
- Current tenant is read from `X-Tenant-ID` request header.
- Service layer always queries and writes scoped by tenant.

## Security

- HTTP Basic authentication enabled.
- Demo user credentials:
	- username: `admin`
	- password: `admin123`

## Run

```bash
mvn spring-boot:run
```

If your local PostgreSQL credentials differ from defaults, set environment variables before running:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/workhub"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="password"
mvn spring-boot:run
```

## Example Request

```bash
curl -X POST http://localhost:8080/api/v1/workspaces \
	-u admin:admin123 \
	-H "Content-Type: application/json" \
	-H "X-Tenant-ID: tenant-a" \
	-d '{"name":"Acme Workspace","ownerEmail":"owner@acme.com"}'
```