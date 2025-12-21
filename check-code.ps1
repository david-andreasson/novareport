#!/usr/bin/env pwsh
# Code Quality Check Script
# Run this before every commit to catch issues early

Write-Host "=== Nova Report Code Quality Check ===" -ForegroundColor Cyan
Write-Host ""

$reportPath = Join-Path $PSScriptRoot "code-quality-report.txt"
$runStart = Get-Date
$timestamp = $runStart.ToString("yyyy-MM-dd HH:mm:ss")
$ErrorActionPreference = "Stop"
$PSDefaultParameterValues['Set-Content:Encoding'] = 'utf8'
$PSDefaultParameterValues['Add-Content:Encoding'] = 'utf8'

Set-Content -Path $reportPath -Value "=== Nova Report Code Quality Report ($timestamp) ===`r`n"

$failures = New-Object System.Collections.Generic.List[string]
$services = @(
    "apps/accounts-service"
    "apps/subscriptions-service"
    "apps/reporter-service"
    "apps/notifications-service"
    "apps/payments-xmr-service"
    "apps/payments-stripe-service"
)

$allPassed = $true

function Resolve-MavenCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory
    )

    $isWindowsPlatform = $IsWindows
    if ($null -eq $isWindowsPlatform) {
        $isWindowsPlatform = ($env:OS -eq 'Windows_NT')
    }

    if ($isWindowsPlatform) {
        $localCandidates = @(
            @{ Path = (Join-Path $WorkingDirectory "mvnw.cmd"); Invoke = "./mvnw.cmd" },
            @{ Path = (Join-Path $WorkingDirectory "mvnw"); Invoke = "./mvnw" }
        )
    } else {
        $localCandidates = @(
            @{ Path = (Join-Path $WorkingDirectory "mvnw"); Invoke = "./mvnw" },
            @{ Path = (Join-Path $WorkingDirectory "mvnw.cmd"); Invoke = "./mvnw.cmd" }
        )
    }

    foreach ($candidate in $localCandidates) {
        if (Test-Path -LiteralPath $candidate.Path) {
            return $candidate.Invoke
        }
    }

    $rootCandidates = @(
        @{ Path = (Join-Path $PSScriptRoot "mvnw.cmd"); Invoke = (Join-Path $PSScriptRoot "mvnw.cmd") },
        @{ Path = (Join-Path $PSScriptRoot "mvnw"); Invoke = (Join-Path $PSScriptRoot "mvnw") }
    )
    foreach ($candidate in $rootCandidates) {
        if (Test-Path -LiteralPath $candidate.Path) {
            return $candidate.Invoke
        }
    }

    $knownFallbackCandidates = @(
        (Join-Path $PSScriptRoot "apps/accounts-service/mvnw.cmd"),
        (Join-Path $PSScriptRoot "apps/accounts-service/mvnw")
    )
    foreach ($candidate in $knownFallbackCandidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $mvn = Get-Command "mvn" -ErrorAction SilentlyContinue
    if ($null -ne $mvn) {
        return "mvn"
    }

    return $null
}

function Get-TestSummaryFromMavenOutput {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Text
    )

    $testMatches = [regex]::Matches($Text, "Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)")
    if ($testMatches.Count -eq 0) {
        return $null
    }

    $m = $testMatches[$testMatches.Count - 1]
    return [pscustomobject]@{
        Run      = [int]$m.Groups[1].Value
        Failures = [int]$m.Groups[2].Value
        Errors   = [int]$m.Groups[3].Value
        Skipped  = [int]$m.Groups[4].Value
    }
}

function Get-JaCoCoCoverageFromService {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ServiceDirectory
    )

    $csvPath = Join-Path $ServiceDirectory "target/site/jacoco/jacoco.csv"
    $xmlPath = Join-Path $ServiceDirectory "target/site/jacoco/jacoco.xml"

    if (Test-Path -LiteralPath $csvPath) {
        try {
            $rows = Import-Csv -LiteralPath $csvPath
        } catch {
            $rows = $null
        }

        if ($null -ne $rows) {
            [long]$instructionMissed = 0
            [long]$instructionCovered = 0
            [long]$lineMissed = 0
            [long]$lineCovered = 0

            foreach ($row in $rows) {
                if ($null -ne $row.INSTRUCTION_MISSED -and $row.INSTRUCTION_MISSED -ne "") {
                    $instructionMissed += [long]$row.INSTRUCTION_MISSED
                }
                if ($null -ne $row.INSTRUCTION_COVERED -and $row.INSTRUCTION_COVERED -ne "") {
                    $instructionCovered += [long]$row.INSTRUCTION_COVERED
                }
                if ($null -ne $row.LINE_MISSED -and $row.LINE_MISSED -ne "") {
                    $lineMissed += [long]$row.LINE_MISSED
                }
                if ($null -ne $row.LINE_COVERED -and $row.LINE_COVERED -ne "") {
                    $lineCovered += [long]$row.LINE_COVERED
                }
            }

            $instructionTotal = $instructionMissed + $instructionCovered
            $lineTotal = $lineMissed + $lineCovered

            $instructionPercent = $null
            if ($instructionTotal -gt 0) {
                $instructionPercent = [math]::Round(($instructionCovered * 100.0) / $instructionTotal, 2)
            }

            $linePercent = $null
            if ($lineTotal -gt 0) {
                $linePercent = [math]::Round(($lineCovered * 100.0) / $lineTotal, 2)
            }

            return [pscustomobject]@{
                ReportPath         = $csvPath
                InstructionCovered = $instructionCovered
                InstructionTotal   = $instructionTotal
                InstructionPercent = $instructionPercent
                LineCovered        = $lineCovered
                LineTotal          = $lineTotal
                LinePercent        = $linePercent
            }
        }
    }

    if (Test-Path -LiteralPath $xmlPath) {
        try {
            [xml]$xml = Get-Content -LiteralPath $xmlPath -Raw
        } catch {
            return $null
        }

        $counters = $xml.report.counter
        if ($null -eq $counters) {
            return $null
        }

        $lineCounter = $counters | Where-Object { $_.type -eq 'LINE' } | Select-Object -First 1
        $instructionCounter = $counters | Where-Object { $_.type -eq 'INSTRUCTION' } | Select-Object -First 1
        if ($null -eq $lineCounter -or $null -eq $instructionCounter) {
            return $null
        }

        [long]$lineMissed = $lineCounter.missed
        [long]$lineCovered = $lineCounter.covered
        [long]$instructionMissed = $instructionCounter.missed
        [long]$instructionCovered = $instructionCounter.covered

        $lineTotal = $lineMissed + $lineCovered
        $instructionTotal = $instructionMissed + $instructionCovered

        $linePercent = $null
        if ($lineTotal -gt 0) {
            $linePercent = [math]::Round(($lineCovered * 100.0) / $lineTotal, 2)
        }

        $instructionPercent = $null
        if ($instructionTotal -gt 0) {
            $instructionPercent = [math]::Round(($instructionCovered * 100.0) / $instructionTotal, 2)
        }

        return [pscustomobject]@{
            ReportPath         = $xmlPath
            InstructionCovered = $instructionCovered
            InstructionTotal   = $instructionTotal
            InstructionPercent = $instructionPercent
            LineCovered        = $lineCovered
            LineTotal          = $lineTotal
            LinePercent        = $linePercent
        }
    }

    return $null
}

foreach ($service in $services) {
    Write-Host "Checking $service..." -ForegroundColor Yellow
    Add-Content -Path $reportPath -Value "--- $service ---"
    
    # Store current location in case Push-Location fails
    $originalLocation = Get-Location
    
    try {
        Push-Location $service -ErrorAction Stop
    } catch {
        Set-Location $originalLocation
        Write-Host "  ✗ Failed to access directory: $_" -ForegroundColor Red
        Add-Content -Path $reportPath -Value "[FAIL] Failed to access directory: $_"
        $failures.Add("${service}: directory access")
        $allPassed = $false
        continue
    }
    
    try {
        $mavenCommand = Resolve-MavenCommand -WorkingDirectory (Get-Location).Path
        if ($null -eq $mavenCommand) {
            Write-Host "  ✗" -ForegroundColor Red
            $msg = "No Maven command found. Expected mvnw/mvnw.cmd in service folder, mvnw/mvnw.cmd in repo root, or mvn on PATH."
            Write-Host "  $msg" -ForegroundColor Red
            Add-Content -Path $reportPath -Value "[FAIL] $msg"
            $failures.Add("${service}: maven not found")
            $allPassed = $false
            continue
        }

        $pomPath = Join-Path (Get-Location).Path "pom.xml"
        if (-not (Test-Path -LiteralPath $pomPath)) {
            Write-Host "  ✗" -ForegroundColor Red
            $msg = "No pom.xml found at: $pomPath"
            Write-Host "  $msg" -ForegroundColor Red
            Add-Content -Path $reportPath -Value "[FAIL] $msg"
            $failures.Add("${service}: pom.xml missing")
            $allPassed = $false
            continue
        }

        Add-Content -Path $reportPath -Value "Maven command: $mavenCommand"
        Write-Host "  → Running verify..." -NoNewline
        try {
            $output = & $mavenCommand -f $pomPath clean verify 2>&1
        } catch {
            Write-Host " ✗" -ForegroundColor Red
            $output = $_ | Out-String
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] mvn clean verify"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("${service}: mvn clean verify")
            $allPassed = $false
            continue
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] mvn clean verify"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("${service}: mvn clean verify")
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        Add-Content -Path $reportPath -Value "[PASS] mvn clean verify"

        $verifyOutputText = ($output | Out-String)
        $testSummary = Get-TestSummaryFromMavenOutput -Text $verifyOutputText
        if ($null -ne $testSummary) {
            $testSummaryText = "Tests run: $($testSummary.Run), Failures: $($testSummary.Failures), Errors: $($testSummary.Errors), Skipped: $($testSummary.Skipped)"
            Add-Content -Path $reportPath -Value $testSummaryText
            Write-Host "    $testSummaryText" -ForegroundColor DarkGray
        } else {
            Add-Content -Path $reportPath -Value "Tests: (summary not found in Maven output)"
        }

        $coverage = Get-JaCoCoCoverageFromService -ServiceDirectory (Get-Location).Path
        if ($null -ne $coverage -and $null -ne $coverage.LinePercent -and $null -ne $coverage.InstructionPercent) {
            $coverageText = "Coverage: Line $($coverage.LinePercent)% ($($coverage.LineCovered)/$($coverage.LineTotal)), Instruction $($coverage.InstructionPercent)% ($($coverage.InstructionCovered)/$($coverage.InstructionTotal)) [$($coverage.ReportPath)]"
            Add-Content -Path $reportPath -Value $coverageText
            Write-Host "    $coverageText" -ForegroundColor DarkGray
        } elseif ($null -ne $coverage) {
            Add-Content -Path $reportPath -Value "Coverage: (JaCoCo report found but totals could not be computed)"
        } else {
            Add-Content -Path $reportPath -Value "Coverage: (no JaCoCo report found)"
        }
        
        Write-Host "  → SpotBugs analysis..." -NoNewline
        try {
            $output = & $mavenCommand -f $pomPath spotbugs:check 2>&1
        } catch {
            Write-Host " ✗" -ForegroundColor Red
            $output = $_ | Out-String
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] mvn spotbugs:check"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("${service}: mvn spotbugs:check")
            $allPassed = $false
            continue
        }
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
            
            Add-Content -Path $reportPath -Value "[FAIL] mvn spotbugs:check"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("${service}: mvn spotbugs:check")
            $allPassed = $false
            continue
        }
        Write-Host " ✓" -ForegroundColor Green
        Add-Content -Path $reportPath -Value "[PASS] mvn spotbugs:check"
        
    } finally {
        Pop-Location
    }
}

Write-Host "Checking apps/frontend..." -ForegroundColor Yellow
Add-Content -Path $reportPath -Value "--- apps/frontend ---"

try {
    Push-Location "apps/frontend" -ErrorAction Stop
} catch {
    Write-Host "  ✗ Failed to access directory: $_" -ForegroundColor Red
    Add-Content -Path $reportPath -Value "[FAIL] Failed to access directory: $_"
    $failures.Add("apps/frontend: directory access")
    $allPassed = $false
}

if ((Get-Location).Path -like "*apps\\frontend") {
    try {
        Write-Host "  → Running lint..." -NoNewline
        $lintHandledInCatch = $false
        try {
            $output = & npm run lint 2>&1
        } catch {
            Write-Host " ✗" -ForegroundColor Red
            $output = $_ | Out-String
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run lint"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run lint")
            $allPassed = $false
            $lintHandledInCatch = $true
        }
        if ($lintHandledInCatch) {
        } elseif ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run lint"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run lint")
            $allPassed = $false
        } else {
            Write-Host " ✓" -ForegroundColor Green
            Add-Content -Path $reportPath -Value "[PASS] npm run lint"
        }

        Write-Host "  → Running build..." -NoNewline
        $buildHandledInCatch = $false
        try {
            $output = & npm run build 2>&1
        } catch {
            Write-Host " ✗" -ForegroundColor Red
            $output = $_ | Out-String
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run build"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run build")
            $allPassed = $false
            $buildHandledInCatch = $true
        }
        if ($buildHandledInCatch) {
        } elseif ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run build"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run build")
            $allPassed = $false
        } else {
            Write-Host " ✓" -ForegroundColor Green
            Add-Content -Path $reportPath -Value "[PASS] npm run build"
        }

        Write-Host "  → Running tests (coverage)..." -NoNewline
        $testsHandledInCatch = $false
        try {
            $output = & npm run test:coverage 2>&1
        } catch {
            Write-Host " ✗" -ForegroundColor Red
            $output = $_ | Out-String
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run test:coverage"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run test:coverage")
            $allPassed = $false
            $testsHandledInCatch = $true
        }
        if ($testsHandledInCatch) {
        } elseif ($LASTEXITCODE -ne 0) {
            Write-Host " ✗" -ForegroundColor Red
            Write-Host $output
            Add-Content -Path $reportPath -Value "[FAIL] npm run test:coverage"
            Add-Content -Path $reportPath -Value $output
            $failures.Add("apps/frontend: npm run test:coverage")
            $allPassed = $false
        } else {
            Write-Host " ✓" -ForegroundColor Green
            Add-Content -Path $reportPath -Value "[PASS] npm run test:coverage"
        }
    } finally {
        Pop-Location
    }
}

$runEnd = Get-Date
$duration = New-TimeSpan -Start $runStart -End $runEnd
$durationText = $duration.ToString("hh':'mm':'ss")

Add-Content -Path $reportPath -Value ""
Add-Content -Path $reportPath -Value "=== Summary ==="
Add-Content -Path $reportPath -Value "Duration: $durationText"
if ($allPassed) {
    Add-Content -Path $reportPath -Value "Overall: PASS"
} else {
    Add-Content -Path $reportPath -Value "Overall: FAIL"
    Add-Content -Path $reportPath -Value "Failures:"
    foreach ($failure in $failures) {
        Add-Content -Path $reportPath -Value "- $failure"
    }
}

Write-Host ""
if ($allPassed) {
    Write-Host "✓ All checks passed! Code is ready to commit." -ForegroundColor Green
    Write-Host "Report written to: $reportPath" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "✗ Some checks failed. Fix the issues before committing." -ForegroundColor Red
    Write-Host "Report written to: $reportPath" -ForegroundColor Cyan
    exit 1
}
