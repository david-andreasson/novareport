#!/usr/bin/env pwsh
# Helper script to manually confirm a payment (for testing)

param(
    [Parameter(Mandatory=$true)]
    [string]$PaymentId
)

$ErrorActionPreference = "Stop"

Write-Host "Confirming payment $PaymentId..." -ForegroundColor Yellow

try {
    Invoke-RestMethod -Uri "http://localhost:8084/api/v1/internal/payments/$PaymentId/confirm" `
        -Method Post `
        -Headers @{"X-INTERNAL-KEY" = "dev-change-me"} | Out-Null
    
    Write-Host "✓ Payment confirmed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "The frontend polling should detect this within 5 seconds." -ForegroundColor Cyan
} catch {
    Write-Host "✗ Failed to confirm payment: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
