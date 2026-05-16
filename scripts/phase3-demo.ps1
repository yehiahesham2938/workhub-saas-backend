# Phase 3 end-to-end demo script (PowerShell)
# Prereqs: app running on http://localhost:8080

$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }
$CorrelationId = "phase3-demo-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    $uri = "$BaseUrl$Path"
    $params = @{
        Method      = $Method
        Uri         = $uri
        Headers     = $Headers
        ContentType = "application/json"
    }
    if ($Body) { $params.Body = $Body }
    return Invoke-RestMethod @params
}

Write-Host "=== Phase 3 Demo ===" -ForegroundColor Cyan
Write-Host "Correlation-Id: $CorrelationId"

$commonHeaders = @{ "X-Correlation-Id" = $CorrelationId }

Write-Host "`n1. Login (tenant-a admin)"
$login = Invoke-Api -Method Post -Path "/auth/login" -Headers $commonHeaders `
    -Body '{"email":"admin@a.com","password":"x"}'
$auth = @{ Authorization = "Bearer $($login.token)"; "X-Correlation-Id" = $CorrelationId }
Write-Host "   tenantId=$($login.tenantId) role=$($login.role)"

Write-Host "`n2. Create project"
$project = Invoke-Api -Method Post -Path "/projects" -Headers $auth `
    -Body "{`"name`":`"Demo Project $(New-Guid)`"}"
Write-Host "   projectId=$($project.id)"

Write-Host "`n3. Metrics snapshot"
$metrics = Invoke-Api -Method Get -Path "/actuator/metrics/workhub.projects.created" -Headers @{}
Write-Host "   workhub.projects.created measurements: $($metrics.measurements | ConvertTo-Json -Compress)"

Write-Host "`n4. Admin audit (first page)"
$audit = Invoke-Api -Method Get -Path "/admin/audit?page=0&size=5" -Headers $auth
Write-Host "   audit events on page: $($audit.content.Count)"

Write-Host "`n5. Quota usage"
$quotas = Invoke-Api -Method Get -Path "/admin/quotas" -Headers $auth
Write-Host "   plan=$($quotas.plan) projects=$($quotas.projects.used)/$($quotas.projects.max)"

Write-Host "`n6. Tenant summary"
$summary = Invoke-Api -Method Get -Path "/admin/tenant/summary" -Headers $auth
Write-Host "   workspaces=$($summary.workspaceCount) projects=$($summary.projectCount)"

Write-Host "`n7. Queue inspection"
$queues = Invoke-Api -Method Get -Path "/admin/queues/dead-letter" -Headers $auth
Write-Host "   DLQ depth=$($queues.messageCount) jobs queue depth=$($queues.jobsQueueDepth)"

Write-Host "`n8. Idempotent job"
$idemKey = "demo-key-$(New-Guid)"
$jobBody = "{`"idempotencyKey`":`"$idemKey`"}"
$job1 = Invoke-Api -Method Post -Path "/jobs" -Headers $auth -Body $jobBody
$job2 = Invoke-Api -Method Post -Path "/jobs" -Headers $auth -Body $jobBody
Write-Host "   same job id: $($job1.id -eq $job2.id) -> $($job1.id)"

Write-Host "`n9. Saga failure (compensation)"
$sagaFail = Invoke-Api -Method Post -Path "/projects/provision-saga" -Headers $auth -Body (@{
    name = "Saga Fail Demo"
    defaultTaskStatuses = @("TODO", "IN_PROGRESS")
    simulateFailure = $true
} | ConvertTo-Json)
Write-Host "   saga status=$($sagaFail.status) step=$($sagaFail.currentStep)"

Write-Host "`n10. Cross-tenant check (login tenant-b, expect 404)"
$loginB = Invoke-Api -Method Post -Path "/auth/login" -Headers $commonHeaders `
    -Body '{"email":"admin@b.com","password":"x"}'
$authB = @{ Authorization = "Bearer $($loginB.token)"; "X-Correlation-Id" = $CorrelationId }
try {
    Invoke-WebRequest -Method Get -Uri "$BaseUrl/projects/$($project.id)" -Headers $authB -ErrorAction Stop
    Write-Host "   ERROR: expected 404" -ForegroundColor Red
} catch {
    Write-Host "   cross-tenant GET status=$($_.Exception.Response.StatusCode.value__)" -ForegroundColor Green
}

Write-Host "`n=== Demo complete ===" -ForegroundColor Cyan
Write-Host "Check logs for cid=$CorrelationId and visit $BaseUrl/actuator/metrics"
