#!/usr/bin/env bash
# Phase 2 (Week 12) end-to-end demo:
#   1. Login Tenant A admin
#   2. Create a job (POST /jobs)  -> messaging publishes JobMessage
#   3. Poll GET /jobs/{id} until DONE / FAILED
#   4. Show cross-tenant denial (Tenant B reading A's job)
#
# Requirements:
#   - jq, curl on PATH
#   - app running on $BASE_URL with PostgreSQL + RabbitMQ reachable
#
# Usage:
#   BASE_URL=http://localhost:8080 ./scripts/phase2-demo.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_A_EMAIL="${ADMIN_A_EMAIL:-admin@a.com}"
ADMIN_B_EMAIL="${ADMIN_B_EMAIL:-admin@b.com}"
PASSWORD="${PASSWORD:-pw}"

step() { printf "\n=== %s ===\n" "$1"; }

login() {
  local email="$1"
  curl -sS -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$PASSWORD\"}" | jq -r .token
}

step "1. Login Tenant A admin ($ADMIN_A_EMAIL)"
TOKEN_A=$(login "$ADMIN_A_EMAIL")
echo "TOKEN_A acquired (len=${#TOKEN_A})"

step "2. Login Tenant B admin ($ADMIN_B_EMAIL)"
TOKEN_B=$(login "$ADMIN_B_EMAIL")
echo "TOKEN_B acquired (len=${#TOKEN_B})"

step "3. Create job under tenant A (expect 202 + status PENDING)"
JOB_JSON=$(curl -sS -X POST "$BASE_URL/jobs" \
  -H "Authorization: Bearer $TOKEN_A")
echo "$JOB_JSON" | jq .
JOB_ID=$(echo "$JOB_JSON" | jq -r .id)
INITIAL_STATUS=$(echo "$JOB_JSON" | jq -r .status)
echo "JOB_ID=$JOB_ID initial status=$INITIAL_STATUS"

step "4. Poll job until terminal state (DONE or FAILED)"
TERMINAL=""
for i in $(seq 1 20); do
  STATUS=$(curl -sS -H "Authorization: Bearer $TOKEN_A" \
    "$BASE_URL/jobs/$JOB_ID" | jq -r .status)
  echo "poll #$i status=$STATUS"
  if [[ "$STATUS" == "DONE" || "$STATUS" == "FAILED" ]]; then
    TERMINAL="$STATUS"
    break
  fi
  sleep 1
done

if [[ -z "$TERMINAL" ]]; then
  echo "FAIL: job did not reach terminal state in time"
  exit 1
fi
echo "Final status: $TERMINAL"

step "5. Cross-tenant read denial: Tenant B reading Tenant A's job (expect 404)"
HTTP_CODE=$(curl -sS -o /tmp/phase2_xtenant_body.json -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN_B" \
  "$BASE_URL/jobs/$JOB_ID")
echo "HTTP $HTTP_CODE"
cat /tmp/phase2_xtenant_body.json | jq . || cat /tmp/phase2_xtenant_body.json
if [[ "$HTTP_CODE" != "404" ]]; then
  echo "FAIL: expected 404 cross-tenant; got $HTTP_CODE"
  exit 1
fi

step "6. Observability: actuator health + correlation id round-trip"
CID="phase2-demo-$(date +%s)"
HEALTH_STATUS=$(curl -sS -H "X-Correlation-Id: $CID" "$BASE_URL/actuator/health" | jq -r .status)
echo "actuator/health status=$HEALTH_STATUS  (sent X-Correlation-Id=$CID)"

READINESS_STATUS=$(curl -sS "$BASE_URL/actuator/health/readiness" | jq -r .status)
LIVENESS_STATUS=$(curl -sS "$BASE_URL/actuator/health/liveness" | jq -r .status)
echo "readiness=$READINESS_STATUS liveness=$LIVENESS_STATUS"

step "DONE"
echo "Job $JOB_ID terminal=$TERMINAL  cross-tenant=404  health=$HEALTH_STATUS readiness=$READINESS_STATUS liveness=$LIVENESS_STATUS"
