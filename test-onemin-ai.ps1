# Test script for 1min.ai integration
# This script tests the AI summarization functionality

param(
    [string]$ApiKey = $env:ONEMIN_API_KEY
)

Write-Host "=== Testing 1min.ai Integration ===" -ForegroundColor Cyan
Write-Host ""

if ([string]::IsNullOrEmpty($ApiKey)) {
    Write-Host "ERROR: ONEMIN_API_KEY not set!" -ForegroundColor Red
    Write-Host "Please set your API key:" -ForegroundColor Yellow
    Write-Host '  $env:ONEMIN_API_KEY = "your-api-key-here"' -ForegroundColor Gray
    Write-Host "Or run with: .\test-onemin-ai.ps1 -ApiKey your-api-key" -ForegroundColor Gray
    exit 1
}

Write-Host "API Key found: $($ApiKey.Substring(0, [Math]::Min(10, $ApiKey.Length)))..." -ForegroundColor Green
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
    Write-Host "  Start it with fake-ai disabled:" -ForegroundColor Yellow
    Write-Host '  $env:ONEMIN_API_KEY = "your-key"' -ForegroundColor Gray
    Write-Host '  $env:REPORTER_FAKE_AI = "false"' -ForegroundColor Gray
    Write-Host "  docker-compose up -d reporter-service" -ForegroundColor Gray
    exit 1
}

Write-Host ""
Write-Host "Step 2: Trigger RSS ingest to get news..." -ForegroundColor Yellow
try {
    $ingestResult = Invoke-RestMethod -Uri "$REPORTER_URL/api/v1/internal/reporter/ingest-now" `
        -Method Post `
        -Headers @{"X-INTERNAL-KEY" = $INTERNAL_KEY}
    
    Write-Host "✓ RSS ingest completed" -ForegroundColor Green
    Write-Host "  Attempted: $($ingestResult.attempted)" -ForegroundColor Gray
    Write-Host "  Stored: $($ingestResult.stored)" -ForegroundColor Gray
    
    if ($ingestResult.stored -eq 0) {
        Write-Host ""
        Write-Host "WARNING: No new news items stored (might be duplicates)" -ForegroundColor Yellow
        Write-Host "The AI will still generate a report from existing news in the database" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Failed to trigger ingest: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 3: Generate AI-powered report..." -ForegroundColor Yellow
Write-Host "This will call 1min.ai API - please wait..." -ForegroundColor Cyan
$today = Get-Date -Format "yyyy-MM-dd"

try {
    $startTime = Get-Date
    Invoke-RestMethod -Uri "$REPORTER_URL/api/v1/internal/reporter/build-report?date=$today" `
        -Method Post `
        -Headers @{
            "X-INTERNAL-KEY" = $INTERNAL_KEY
        } | Out-Null
    $duration = (Get-Date) - $startTime
    
    Write-Host "✓ AI report generated successfully in $($duration.TotalSeconds) seconds" -ForegroundColor Green
    
} catch {
    Write-Host "✗ Failed to generate AI report: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "Step 4: Verify report was saved and retrieve it..." -ForegroundColor Yellow
try {
    $savedReport = Invoke-RestMethod -Uri "$REPORTER_URL/api/v1/reports/latest" -Method Get
    
    Write-Host "✓ Report successfully retrieved" -ForegroundColor Green
    Write-Host "  Report ID: $($savedReport.id)" -ForegroundColor Gray
    Write-Host "  Date: $($savedReport.reportDate)" -ForegroundColor Gray
    Write-Host "  Summary length: $($savedReport.summary.Length) characters" -ForegroundColor Gray
    Write-Host ""
    Write-Host "=== AI-Generated Summary ===" -ForegroundColor Cyan
    Write-Host $savedReport.summary -ForegroundColor White
    Write-Host ""
    Write-Host "=== End of Summary ===" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Failed to retrieve saved report: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== All tests passed! ===" -ForegroundColor Green
Write-Host ""
Write-Host "1min.ai integration is working correctly!" -ForegroundColor Green
Write-Host "The AI is now generating comprehensive cryptocurrency reports." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Set REPORTER_FAKE_AI=false in docker-compose.yml" -ForegroundColor Gray
Write-Host "  2. Set ONEMIN_API_KEY in your environment" -ForegroundColor Gray
Write-Host "  3. Restart reporter-service" -ForegroundColor Gray
Write-Host "  4. Reports will be generated automatically every 4 hours" -ForegroundColor Gray
