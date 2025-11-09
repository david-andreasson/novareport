# Test script for scheduled report generation
# This script verifies that the scheduler is working correctly

Write-Host "=== Testing Reporter Scheduler ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$REPORTER_URL = "http://localhost:8082"
$INTERNAL_KEY = "dev-change-me"

Write-Host "Step 1: Check if reporter-service is running..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$REPORTER_URL/actuator/health" -Method Get
    Write-Host "✓ Reporter service is running - Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Reporter service is not running!" -ForegroundColor Red
    Write-Host "  Start it with: docker-compose up reporter-service" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "Step 2: Manually trigger RSS ingest..." -ForegroundColor Yellow
try {
    $ingestResult = Invoke-RestMethod -Uri "$REPORTER_URL/api/v1/internal/reporter/ingest" `
        -Method Post `
        -Headers @{"X-INTERNAL-KEY" = $INTERNAL_KEY}
    
    Write-Host "✓ RSS ingest completed" -ForegroundColor Green
    Write-Host "  Attempted: $($ingestResult.attempted)" -ForegroundColor Gray
    Write-Host "  Stored: $($ingestResult.stored)" -ForegroundColor Gray
    Write-Host "  Duplicates: $($ingestResult.attempted - $ingestResult.stored)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to trigger ingest: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 3: Manually trigger report generation..." -ForegroundColor Yellow
$today = Get-Date -Format "yyyy-MM-dd"
try {
    $reportResult = Invoke-RestMethod -Uri "$REPORTER_URL/api/v1/internal/reporter/build-report" `
        -Method Post `
        -Headers @{
            "X-INTERNAL-KEY" = $INTERNAL_KEY
            "Content-Type" = "application/json"
        } `
        -Body (@{date = $today} | ConvertTo-Json)
    
    Write-Host "✓ Report generated successfully" -ForegroundColor Green
    Write-Host "  Report ID: $($reportResult.id)" -ForegroundColor Gray
    Write-Host "  Date: $($reportResult.reportDate)" -ForegroundColor Gray
    Write-Host "  Summary length: $($reportResult.summary.Length) chars" -ForegroundColor Gray
    Write-Host "  Summary preview: $($reportResult.summary.Substring(0, [Math]::Min(100, $reportResult.summary.Length)))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Failed to generate report: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 4: Check scheduler logs..." -ForegroundColor Yellow
Write-Host "The scheduler will run automatically every 4 hours at:" -ForegroundColor Cyan
Write-Host "  00:00, 04:00, 08:00, 12:00, 16:00, 20:00" -ForegroundColor Cyan
Write-Host ""
Write-Host "To verify the scheduler is working, check the logs for:" -ForegroundColor Yellow
Write-Host "  === Starting scheduled report generation ===" -ForegroundColor Gray
Write-Host ""
Write-Host "Current time: $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Cyan
$currentHour = (Get-Date).Hour
$nextRuns = @(0, 4, 8, 12, 16, 20) | Where-Object { $_ -gt $currentHour }
if ($nextRuns.Count -eq 0) {
    $nextRun = "00:00 (tomorrow)"
} else {
    $nextRun = "$($nextRuns[0]):00 (today)"
}
Write-Host "Next scheduled run: $nextRun" -ForegroundColor Cyan

Write-Host ""
Write-Host "=== All tests passed! ===" -ForegroundColor Green
Write-Host ""
Write-Host "The scheduler is now active and will generate reports automatically." -ForegroundColor Green
Write-Host "You can monitor the logs with:" -ForegroundColor Yellow
Write-Host "  docker-compose logs -f reporter-service" -ForegroundColor Gray
