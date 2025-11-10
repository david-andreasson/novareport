# Testing Payment Flow

This guide explains how to test the payment subscription flow in NovaReport.

## Prerequisites

- All services running via `docker-compose up -d`
- Frontend accessible at http://localhost:5173

## Automated Backend Test

Run the automated test script to verify the entire backend flow:

```powershell
.\test-payment-flow.ps1
```

This will:
1. Create a test user
2. Create a payment
3. Verify PENDING status
4. Confirm the payment (simulating Monero payment)
5. Verify CONFIRMED status
6. Verify subscription activation

## Manual Frontend Test

### Step 1: Create Account
1. Open http://localhost:5173
2. Click "Skapa konto"
3. Fill in the form and register

### Step 2: Navigate to Subscribe
1. After login, click "Prenumerera" in the sidebar
2. You should see two subscription plans:
   - **Månad**: 0.05 XMR for 30 days
   - **År**: 0.50 XMR for 365 days (17% savings)

### Step 3: Select a Plan
1. Click "Välj månad" or "Välj år"
2. The payment details should appear:
   - Monero address (95 characters starting with '4')
   - Amount in XMR
   - Expiry time (24 hours from now)
   - QR code placeholder (TODO)

### Step 4: Copy Payment Address
1. Click the "Kopiera" button
2. You should see "Adress kopierad!" message
3. The Monero address is now in your clipboard

### Step 5: Simulate Payment
Since this is a fake payment system, you need to manually confirm the payment:

1. Copy the Payment ID from the browser console (or check the network tab)
2. Run the confirmation script:
   ```powershell
   .\confirm-payment.ps1 -PaymentId <payment-id>
   ```
3. The frontend should detect the confirmation within 5 seconds
4. You should see "✓ Betalning bekräftad!" message
5. Click "Visa rapporter" to go to the report page

### Step 6: Verify Subscription
1. Go to "Min profil"
2. Verify that subscription status shows "Aktiv"
3. Check the expiry date (should be 30 or 365 days from now)

## Payment Flow States

The payment UI has the following states:

- **idle**: Initial state, showing plan selection
- **selecting**: Loading state while creating payment
- **pending**: Payment created, waiting for user to send XMR
- **polling**: Actively polling for payment confirmation
- **confirmed**: Payment confirmed, subscription active
- **expired**: Payment took too long (10 minutes timeout)
- **error**: Something went wrong

## Polling Behavior

- Polls every 5 seconds
- Maximum 120 attempts (10 minutes)
- Automatically stops when payment is confirmed or failed
- Shows "⏳ Väntar på betalning..." while polling

## Notes

- This is a **fake payment system** for development
- Real Monero integration will be implemented later
- The QR code is currently a placeholder
- Payment addresses are randomly generated (not real Monero addresses)
