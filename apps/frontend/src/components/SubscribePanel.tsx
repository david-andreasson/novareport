import type { JSX } from 'react'

import type { PaymentState } from '../App'

type SubscribePanelProps = {
  paymentState: PaymentState
  message: JSX.Element | null
  onSelectPlan: (plan: 'monthly' | 'yearly') => void
  onNavigateToReport: () => void
  onResetPayment: () => void
  onCopyPaymentAddress: (address: string) => void
}

export function SubscribePanel({
  paymentState,
  message,
  onSelectPlan,
  onNavigateToReport,
  onResetPayment,
  onCopyPaymentAddress,
}: SubscribePanelProps) {
  const hasPaymentDetails =
    (paymentState.phase === 'pending' || paymentState.phase === 'polling') &&
    paymentState.payment

  let paymentMoneroUri: string | null = null
  let paymentQrUrl: string | null = null

  if (hasPaymentDetails && paymentState.payment) {
    const { paymentAddress, amountXmr } = paymentState.payment
    paymentMoneroUri = `monero:${paymentAddress}?tx_amount=${amountXmr}`
    paymentQrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(
      paymentMoneroUri,
    )}`
  }

  return (
    <>
      <h2>Prenumerera</h2>
      <p className="auth-note">Välj en plan och betala med Monero (XMR)</p>
      {message}
      {paymentState.phase === 'idle' || paymentState.phase === 'selecting' ? (
        <div className="subscription-plans">
          <div className="plan-card">
            <h3>Månad</h3>
            <div className="plan-price">
              <span className="price-amount">0.01</span>
              <span className="price-currency">XMR</span>
            </div>
            <p className="plan-description">Tillgång i 30 dagar</p>
            <button
              className="pill-button"
              type="button"
              onClick={() => onSelectPlan('monthly')}
              disabled={paymentState.phase === 'selecting'}
            >
              Välj månad
            </button>
          </div>
          <div className="plan-card plan-card--featured">
            <span className="plan-badge">Bäst värde</span>
            <h3>År</h3>
            <div className="plan-price">
              <span className="price-amount">0.50</span>
              <span className="price-currency">XMR</span>
            </div>
            <p className="plan-description">Tillgång i 365 dagar</p>
            <p className="plan-savings">Spara 17% jämfört med månad</p>
            <button
              className="pill-button"
              type="button"
              onClick={() => onSelectPlan('yearly')}
              disabled={paymentState.phase === 'selecting'}
            >
              Välj år
            </button>
          </div>
        </div>
      ) : null}
      {paymentState.phase === 'pending' || paymentState.phase === 'polling' ? (
        <div className="payment-details">
          <h3>Skicka betalning</h3>
          <p className="auth-note">
            Skicka exakt <strong>{paymentState.payment?.amountXmr} XMR</strong> till adressen nedan
          </p>
          <div className="payment-address">
            <label>Monero-adress</label>
            <code className="payment-address-code">{paymentState.payment?.paymentAddress}</code>
            <button
              className="copy-button"
              type="button"
              onClick={() => {
                const address = paymentState.payment?.paymentAddress
                if (address) {
                  onCopyPaymentAddress(address)
                }
              }}
            >
              Kopiera
            </button>
          </div>
          <div className="payment-qr">
            {paymentQrUrl && paymentMoneroUri ? (
              <>
                <img
                  src={paymentQrUrl}
                  alt="Monero QR-kod för betalning"
                  className="payment-qr-image"
                />
                <p className="auth-note">
                  Skanna QR-koden i din Monero-plånbok eller{' '}
                  <a href={paymentMoneroUri}>öppna betalning direkt</a>.
                </p>
              </>
            ) : (
              <p className="auth-note">QR-kod kan inte genereras just nu.</p>
            )}
          </div>
          <p className="auth-note">
            Betalningen bekräftas automatiskt när din transaktion fått tillräckligt många
            konfirmationer i Monero-nätverket. Det kan ta upp till cirka 20 minuter.
          </p>
          {paymentState.payment?.expiresAt && (
            <p className="payment-expiry">
              Betalningen går ut:{' '}
              {new Date(paymentState.payment.expiresAt).toLocaleString('sv-SE')}
            </p>
          )}
          {paymentState.phase === 'polling' && (
            <p className="payment-status">⏳ Väntar på betalning...</p>
          )}
        </div>
      ) : null}
      {paymentState.phase === 'confirmed' ? (
        <div className="payment-success">
          <h3>✓ Betalning bekräftad!</h3>
          <p>Din prenumeration är nu aktiv.</p>
          <button className="pill-button" type="button" onClick={onNavigateToReport}>
            Visa rapporter
          </button>
        </div>
      ) : null}
      {paymentState.phase === 'expired' ? (
        <div className="payment-error">
          <h3>Betalningen gick ut</h3>
          <p>Betalningen tog för lång tid. Försök igen.</p>
          <button className="pill-button" type="button" onClick={onResetPayment}>
            Försök igen
          </button>
        </div>
      ) : null}
      {paymentState.phase === 'error' && paymentState.error ? (
        <div className="payment-error">
          <p>{paymentState.error}</p>
          <button className="pill-button" type="button" onClick={onResetPayment}>
            Försök igen
          </button>
        </div>
      ) : null}
    </>
  )
}
