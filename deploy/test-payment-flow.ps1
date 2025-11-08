# Test Payment Flow Script
# Kör detta i PowerShell för att testa hela betalningsflödet

Write-Host "=== Nova Report Payment Flow Test ===" -ForegroundColor Cyan
Write-Host ""

# 1. Logga in eller registrera användare
Write-Host "1. Loggar in..." -ForegroundColor Yellow
try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body (@{
            email = "test@example.com"
            password = "Password123!"
        } | ConvertTo-Json)
    
    $token = $loginResponse.accessToken
    Write-Host "✓ Inloggad!" -ForegroundColor Green
} catch {
    Write-Host "Användare finns inte, registrerar..." -ForegroundColor Yellow
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8080/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body (@{
            email = "test@example.com"
            password = "Password123!"
            firstName = "Test"
            lastName = "User"
        } | ConvertTo-Json)
    
    $token = $registerResponse.accessToken
    Write-Host "✓ Användare registrerad!" -ForegroundColor Green
}
Write-Host "✓ Token mottagen" -ForegroundColor Gray
Write-Host ""

# 2. Skapa betalning
Write-Host "2. Skapar betalning..." -ForegroundColor Yellow
$paymentResponse = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/create" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json" `
    -Body (@{
        plan = "monthly"
        amountXmr = 0.05
    } | ConvertTo-Json)

$paymentId = $paymentResponse.paymentId
$paymentAddress = $paymentResponse.paymentAddress
Write-Host "✓ Betalning skapad!" -ForegroundColor Green
Write-Host "Payment ID: $paymentId" -ForegroundColor Gray
Write-Host "Payment Address: $paymentAddress" -ForegroundColor Gray
Write-Host "Amount: $($paymentResponse.amountXmr) XMR" -ForegroundColor Gray
Write-Host ""

# 3. Bekräfta betalning
Write-Host "3. Bekräftar betalning (simulerar att användaren betalat)..." -ForegroundColor Yellow
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/internal/payments/$paymentId/confirm" `
    -Method Post `
    -Headers @{ "X-INTERNAL-KEY" = "dev-change-me" } | Out-Null

Write-Host "✓ Betalning bekräftad!" -ForegroundColor Green
Write-Host ""

# 4. Kontrollera prenumerationsstatus
Write-Host "4. Kontrollerar prenumerationsstatus..." -ForegroundColor Yellow
$subsResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/subscriptions/me/has-access" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

if ($subsResponse.hasAccess) {
    Write-Host "✓ Prenumeration är aktiv!" -ForegroundColor Green
} else {
    Write-Host "✗ Prenumeration är INTE aktiv!" -ForegroundColor Red
}
Write-Host ""

# 5. Hämta senaste rapporten
Write-Host "5. Hämtar senaste rapporten..." -ForegroundColor Yellow
try {
    $reportResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/reports/latest" `
        -Method Get `
        -Headers @{ Authorization = "Bearer $token" }
    
    Write-Host "✓ Rapport hämtad!" -ForegroundColor Green
    Write-Host "Report ID: $($reportResponse.id)" -ForegroundColor Gray
    Write-Host "Date: $($reportResponse.reportDate)" -ForegroundColor Gray
    Write-Host "Summary: $($reportResponse.summary.Substring(0, 100))..." -ForegroundColor Gray
} catch {
    Write-Host "✗ Ingen rapport tillgänglig än (detta är OK för en ny installation)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "=== Test slutförd! ===" -ForegroundColor Cyan
