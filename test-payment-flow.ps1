#!/usr/bin/env pwsh
# Test script for payment flow

$ErrorActionPreference = "Stop"

Write-Host "=== Testing Payment Flow ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Create test user
Write-Host "Step 1: Creating test user..." -ForegroundColor Yellow
try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8080/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body (@{
            email = "payment-test-$(Get-Random)@example.com"
            password = "Test123!"
            firstName = "Payment"
            lastName = "Test"
        } | ConvertTo-Json)
    
    $token = $registerResponse.accessToken
    Write-Host "✓ User created and logged in" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Failed to create user: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Create payment
Write-Host "Step 2: Creating payment..." -ForegroundColor Yellow
try {
    $paymentResponse = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/create" `
        -Method Post `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $token"
        } `
        -Body (@{
            plan = "monthly"
            amountXmr = "0.05"
        } | ConvertTo-Json)
    
    $paymentId = $paymentResponse.paymentId
    Write-Host "✓ Payment created!" -ForegroundColor Green
    Write-Host "  Payment ID: $paymentId" -ForegroundColor Gray
    Write-Host "  Monero Address: $($paymentResponse.paymentAddress)" -ForegroundColor Gray
    Write-Host "  Amount: $($paymentResponse.amountXmr) XMR" -ForegroundColor Gray
    Write-Host "  Expires At: $($paymentResponse.expiresAt)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "✗ Failed to create payment: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Check initial status
Write-Host "Step 3: Checking initial payment status..." -ForegroundColor Yellow
try {
    $statusResponse = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/$paymentId/status" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $token"}
    
    Write-Host "✓ Status: $($statusResponse.status)" -ForegroundColor Green
    if ($statusResponse.status -ne "PENDING") {
        Write-Host "✗ Expected PENDING status, got $($statusResponse.status)" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
} catch {
    Write-Host "✗ Failed to get payment status: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Simulate payment confirmation (fake backend)
Write-Host "Step 4: Simulating payment confirmation..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8084/api/v1/internal/payments/$paymentId/confirm" `
        -Method Post `
        -Headers @{"X-INTERNAL-KEY" = "dev-change-me"} | Out-Null
    
    Write-Host "✓ Payment confirmed via internal API" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Failed to confirm payment: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Verify confirmed status
Write-Host "Step 5: Verifying confirmed status..." -ForegroundColor Yellow
try {
    $statusResponse = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/$paymentId/status" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $token"}
    
    Write-Host "✓ Status: $($statusResponse.status)" -ForegroundColor Green
    if ($statusResponse.status -ne "CONFIRMED") {
        Write-Host "✗ Expected CONFIRMED status, got $($statusResponse.status)" -ForegroundColor Red
        exit 1
    }
    Write-Host "  Confirmed At: $($statusResponse.confirmedAt)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "✗ Failed to get payment status: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 6: Check subscription status
Write-Host "Step 6: Checking subscription status..." -ForegroundColor Yellow
try {
    $subsResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/subscriptions/me/has-access" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $token"}
    
    Write-Host "✓ Has Access: $($subsResponse.hasAccess)" -ForegroundColor Green
    if ($subsResponse.hasAccess -ne $true) {
        Write-Host "✗ Expected hasAccess to be true" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
} catch {
    Write-Host "✗ Failed to check subscription: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "=== All Tests Passed! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Payment flow works correctly:" -ForegroundColor Cyan
Write-Host "  1. User created ✓" -ForegroundColor Gray
Write-Host "  2. Payment created ✓" -ForegroundColor Gray
Write-Host "  3. Payment status PENDING ✓" -ForegroundColor Gray
Write-Host "  4. Payment confirmed ✓" -ForegroundColor Gray
Write-Host "  5. Payment status CONFIRMED ✓" -ForegroundColor Gray
Write-Host "  6. Subscription activated ✓" -ForegroundColor Gray
Write-Host ""
Write-Host "You can now test the frontend at http://localhost:5173" -ForegroundColor Yellow
Write-Host "Use the 'Prenumerera' page to test the UI flow." -ForegroundColor Yellow
