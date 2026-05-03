# Observability — Phase 2

This document explains every observability surface this service exposes and how to verify each one in under 5 minutes.

## 1. Actuator endpoints

Configured in [application.yml](src/main/resources/application.yml):

| Endpoint                              | Purpose                              | Auth required |
| ------------------------------------- | ------------------------------------ | ------------- |
| `/actuator/health`                    | Aggregate health (UP/DOWN)           | no            |
| `/actuator/health/readiness`          | Pod readiness probe (DB included)    | no            |
| `/actuator/health/liveness`           | Pod liveness probe                   | no            |
| `/actuator/info`                      | Build/app info                       | no            |
| `/actuator/metrics`                   | Metric registry index                | no            |
| `/actuator/metrics/{name}`            | Single metric value                  | no            |

The matching `permitAll()` rules live in [SecurityConfig.java:41](src/main/java/com/workhub/saasbackend/security/SecurityConfig.java#L41).

### Verify

```bash
curl -s http://localhost:8080/actuator/health           | jq .
curl -s http://localhost:8080/actuator/health/readiness | jq .
curl -s http://localhost:8080/actuator/health/liveness  | jq .
curl -s http://localhost:8080/actuator/info             | jq .
curl -s http://localhost:8080/actuator/metrics          | jq '.names | length'
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq .
```

### Sample expected payloads

`/actuator/health`:
```json
{ "status": "UP" }
```

`/actuator/health/readiness` (group includes `readinessState` and `db`):
```json
{ "status": "UP" }
```

`/actuator/health/liveness`:
```json
{ "status": "UP" }
```

`/actuator/info`:
```json
{ "app": { "name": "workhub-saas-backend", "phase": "phase2-week12", "description": "Multi-tenant SaaS backend" } }
```

`/actuator/metrics/jvm.memory.used` (fragment):
```json
{ "name": "jvm.memory.used", "measurements": [{ "statistic": "VALUE", "value": 1.23e8 }] }
```

## 2. Correlation ID end-to-end

- Filter: [CorrelationIdFilter.java](src/main/java/com/workhub/saasbackend/observability/CorrelationIdFilter.java)
  - Reads `X-Correlation-Id` from request, generates a UUID if missing.
  - Stores it in MDC under key `correlationId`.
  - Echoes it back on the response as `X-Correlation-Id`.
- Tenant: [TenantFilter.java](src/main/java/com/workhub/saasbackend/security/TenantFilter.java) puts the JWT's `tenantId` claim into MDC under key `tenantId`.
- Log pattern: [application.yml](src/main/resources/application.yml) `logging.pattern.console` emits
  `cid=%X{correlationId:-} tenant=%X{tenantId:-} job=%X{jobId:-}` on every line.

### Verify request → response → log

```bash
CID="demo-$(date +%s)"
curl -is -H "X-Correlation-Id: $CID" http://localhost:8080/actuator/health | grep -i "X-Correlation-Id"
# Expected response header: X-Correlation-Id: demo-<timestamp>
```

In the running app's stdout, locate a line for that request — every log line will contain `cid=demo-<timestamp>`. Example:

```
2026-05-03 20:35:01.123 INFO  [http-nio-8080-exec-1] cid=demo-1714760101 tenant=- job=- c.w.s.controller.AuthController - login attempt
```

### Verify tenant tag in logs

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@a.com","password":"pw"}' | jq -r .token)

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/projects > /dev/null
# Server log lines for that request include: tenant=tenant-a
```

## 3. Async job lifecycle (messaging observability)

Producer ([JobProducer.java](src/main/java/com/workhub/saasbackend/messaging/JobProducer.java)) logs:
```
published job message jobId=... tenantId=...
```

Consumer ([JobConsumer.java](src/main/java/com/workhub/saasbackend/messaging/JobConsumer.java)) sets MDC keys `jobId` and `tenantId`, then logs:
```
job transition: PENDING -> PROCESSING
job transition: PROCESSING -> DONE
```
Failures log:
```
job failed   (with stack trace and FAILED status persisted with errorMessage)
```

### Verify

Run the bundled demo:
```bash
BASE_URL=http://localhost:8080 ./scripts/phase2-demo.sh
```

Or manually:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@a.com","password":"pw"}' | jq -r .token)

JOB_ID=$(curl -s -X POST http://localhost:8080/jobs \
  -H "Authorization: Bearer $TOKEN" | jq -r .id)

for i in $(seq 1 10); do
  STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/jobs/$JOB_ID | jq -r .status)
  echo "poll #$i status=$STATUS"
  [[ "$STATUS" == "DONE" || "$STATUS" == "FAILED" ]] && break
  sleep 1
done
```

Expected: `PENDING → PROCESSING → DONE` within a few seconds, mirrored in server logs with `job=<UUID>` and `tenant=<id>` MDC tags.

## 4. Reliability surfaces

Wired in [RabbitMQConfig.java](src/main/java/com/workhub/saasbackend/config/RabbitMQConfig.java) and [application.yml](src/main/resources/application.yml):

- Listener container retry: 3 attempts, exponential backoff (`initial 500ms × 2 → max 5s`).
- `default-requeue-rejected: false` — failed messages do not loop the queue forever.
- Dead-letter exchange `jobs.dlx` + queue `jobs.dlq` for terminal failures.
- Producer publish failure does not crash the API: [JobProducer.send](src/main/java/com/workhub/saasbackend/messaging/JobProducer.java) catches `AmqpException`, logs, leaves the job row in `PENDING` so a retry can resend.
- Consumer failures persist `FAILED` + `errorMessage` to the job row before the message is acked/rejected.

### Verify retry / DLQ in RabbitMQ UI

1. Open `http://localhost:15672` (default user/pass `guest`/`guest`).
2. After the demo run, confirm `jobs.queue` shows publish/deliver counters increasing.
3. Force a failure (e.g., temporarily throw inside `JobConsumer.handleJobMessage`) and observe `jobs.dlq` accumulate after 3 retries.

## 5. Troubleshooting

| Symptom                                                         | Likely cause                                          | Fix                                                                      |
| --------------------------------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------ |
| `/actuator/health/readiness` returns 404                        | Probes not enabled                                    | Confirm `management.endpoint.health.probes.enabled: true` in app yml.    |
| `/actuator/health` returns DOWN with `db` component             | Postgres unreachable                                  | Verify `DB_URL`, port, credentials; restart container.                   |
| Logs show `cid=-` on requests                                   | Filter not in chain                                   | `CorrelationIdFilter` must be registered before security; see SecurityConfig. |
| Logs show `tenant=-` for authenticated requests                 | Token missing `tenantId` claim                        | Re-login; check `JwtService.generateToken` includes `tenantId`.          |
| Job stuck `PENDING`                                             | Rabbit unreachable or consumer not running            | Check producer logs for "failed to publish"; check `jobs.queue` consumer count. |
| Same message appears repeatedly in DLQ                          | Bad payload                                           | Inspect DLQ message; consumer raises `AmqpRejectAndDontRequeueException` for malformed messages. |
| `403` on `/actuator/health/*` from outside cluster              | Auth filter blocking probe                            | `permitAll()` on `/actuator/health/**` already configured; check reverse proxy. |

## 6. Full Phase 2 verification checklist

- [ ] `mvn clean test` passes.
- [ ] App starts; `/actuator/health` is `UP`.
- [ ] `/actuator/health/readiness` and `/actuator/health/liveness` both `UP`.
- [ ] `./scripts/phase2-demo.sh` reaches the `DONE` line.
- [ ] Server logs show a `cid=<id> tenant=<id> job=<id>` line for the demo run.
- [ ] Cross-tenant read of the demo job returns `404`.
