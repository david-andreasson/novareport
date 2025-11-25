#!/usr/bin/env pwsh
# Code Quality Check Script
# Run this before every commit to catch issues early

Write-Host "=== Nova Report Code Quality Check ===" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$services = @(
    "apps/accounts-service"
    "apps/subscriptions-service"
    "apps/reporter-service"
    "apps/notifications-service"
    "apps/payments-xmr-service"
)

$allPassed = $true

foreach ($service in $services) {
    Write-Host "Checking $service..." -ForegroundColor Yellow
    
    # Store current location in case Push-Location fails
    $originalLocation = Get-Location
    
    try {
        Push-Location $service -ErrorAction Stop
    } catch {
        Write-Host "  ✗ Failed to access directory: $_" -ForegroundColor Red
        $allPassed = $false
        continue
    }
    
    try {
        # 1. Compile
        Write-Host "  → Compiling..." -NoNewline
        $output = & ./mvnw clean compile 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        
        # 2. SpotBugs analysis
        Write-Host "  → SpotBugs analysis..." -NoNewline
        $output = & ./mvnw spotbugs:check 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host ""
            Write-Host "SpotBugs found bugs in the code:" -ForegroundColor Yellow
            Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
            
            # Extract bug summary
            $bugLines = $output | Select-String -Pattern "\[ERROR\]" | Select-Object -First 20
            foreach ($line in $bugLines) {
                Write-Host $line -ForegroundColor Red
            }
            
            Write-Host ""
            Write-Host "To see all details, run:" -ForegroundColor Cyan
            Write-Host "  cd $service && ./mvnw spotbugs:gui" -ForegroundColor White
            Write-Host ""
            
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        
        # 3. Run tests
        Write-Host "  → Running tests..." -NoNewline
        $output = & ./mvnw test 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        
        # 4. Package (creates JAR)
        Write-Host "  → Building package..." -NoNewline
        $output = & ./mvnw package -DskipTests 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        
    } finally {
        Pop-Location
    }
}

Write-Host ""
if ($allPassed) {
    Write-Host "✓ All checks passed! Code is ready to commit." -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ Some checks failed. Fix the issues before committing." -ForegroundColor Red
    exit 1
}
