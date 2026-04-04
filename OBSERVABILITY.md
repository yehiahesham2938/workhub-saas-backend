---
alwaysApply: false
---
## Actuator Endpoints

- Enabled: health, info, metrics
- Publicly accessible per security config.

### Test
```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/info
curl -s http://localhost:8080/actuator/metrics
```

## Correlation ID

- Incoming requests receive or propagate `X-Correlation-Id`.
- Value is stored in MDC under `correlationId` for log correlation.

### Test
```bash
curl -i http://localhost:8080/actuator/health
# Observe X-Correlation-Id header in response and correlated logs
```

