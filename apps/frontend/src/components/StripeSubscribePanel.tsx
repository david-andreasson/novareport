import type { JSX } from 'react'
import { useEffect, useRef, useState } from 'react'

export type StripePaymentState = {
  phase: 'idle' | 'intentCreated' | 'processing' | 'confirmed' | 'error'
  selectedPlan: 'monthly' | 'yearly' | null
  paymentId: string | null
  clientSecret: string | null
  amountFiat: number | null
  currencyFiat: string | null
  error?: string
}

type StripeSubscribePanelProps = {
  state: StripePaymentState
  message: JSX.Element | null
  onSelectPlan: (plan: 'monthly' | 'yearly') => void
  onPaymentSuccess: () => void
  onPaymentError: (message: string) => void
}

declare global {
  interface Window {
    APP_CONFIG?: {
      INTERNAL_API_KEY?: string
      STRIPE_PUBLISHABLE_KEY?: string
    }
    Stripe?: (publishableKey: string) => any
  }
}

export function StripeSubscribePanel({
  state,
  message,
  onSelectPlan,
  onPaymentSuccess,
  onPaymentError,
}: StripeSubscribePanelProps) {
  const [localError, setLocalError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const containerRef = useRef<HTMLDivElement | null>(null)
  const stripeRef = useRef<any>(null)
  const elementsRef = useRef<any>(null)
  const paymentElementRef = useRef<any>(null)

  const hasIntent = state.phase === 'intentCreated' && !!state.clientSecret

  useEffect(() => {
    setLocalError(null)

    if (!hasIntent || !state.clientSecret) {
      return
    }

    if (typeof window === 'undefined') {
      setLocalError('Stripe kan inte laddas i denna miljö.')
      return
    }

    const publishableKey = window.APP_CONFIG?.STRIPE_PUBLISHABLE_KEY ?? ''
    if (!publishableKey) {
      setLocalError('Stripe publishable key saknas i konfigurationen.')
      return
    }

    if (!window.Stripe) {
      setLocalError('Stripe.js kunde inte laddas (saknar https://js.stripe.com/v3).')
      return
    }

    if (!stripeRef.current) {
      stripeRef.current = window.Stripe(publishableKey)
    }

    const stripe = stripeRef.current
    const elements = stripe.elements({ clientSecret: state.clientSecret })
    elementsRef.current = elements

    const paymentElement = elements.create('payment')
    paymentElementRef.current = paymentElement

    if (containerRef.current) {
      paymentElement.mount(containerRef.current)
    }

    return () => {
      if (paymentElementRef.current) {
        paymentElementRef.current.unmount()
      }
    }
  }, [hasIntent, state.clientSecret])

  const handleSubmit: React.FormEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault()

    if (!stripeRef.current || !elementsRef.current) {
      setLocalError('Stripe är inte redo ännu. Försök igen om en liten stund.')
      return
    }

    setIsSubmitting(true)
    setLocalError(null)

    try {
      const stripe = stripeRef.current
      const elements = elementsRef.current

      const result = await stripe.confirmPayment({
        elements,
        redirect: 'if_required',
      })

      if (result.error) {
        const message = result.error.message ?? 'Betalningen kunde inte genomföras.'
        setLocalError(message)
        onPaymentError(message)
        return
      }

      const status: string | undefined = result.paymentIntent?.status
      if (status === 'succeeded') {
        onPaymentSuccess()
      } else if (status === 'processing') {
        onPaymentSuccess()
      } else {
        const message = 'Betalningen kunde inte bekräftas. Försök igen.'
        setLocalError(message)
        onPaymentError(message)
      }
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Ett oväntat fel inträffade vid kortbetalning.'
      setLocalError(message)
      onPaymentError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const formatAmount = () => {
    if (state.amountFiat == null || !state.currencyFiat) return null
    const amountInMajor = (state.amountFiat / 100).toFixed(0)
    return `${amountInMajor} ${state.currencyFiat.toUpperCase()}`
  }

  return (
    <>
      <p className="auth-note">
        Välj plan och betala med kort via Stripe.
      </p>
      {message}

      {(state.phase === 'idle' || state.phase === 'error') && (
        <div className="subscription-plans">
          <div className="plan-card">
            <h3>Månad</h3>
            <div className="plan-price">
              <span className="price-amount">49</span>
              <span className="price-currency">kr/månad</span>
            </div>
            <p className="plan-description">Tillgång i 30 dagar (kortbetalning)</p>
            <button
              className="pill-button"
              type="button"
              onClick={() => onSelectPlan('monthly')}
              disabled={isSubmitting}
            >
              Betala 49 kr med kort
            </button>
          </div>
          <div className="plan-card plan-card--featured">
            <span className="plan-badge">Bäst värde</span>
            <h3>År</h3>
            <div className="plan-price">
              <span className="price-amount">499</span>
              <span className="price-currency">kr/år</span>
            </div>
            <p className="plan-description">Tillgång i 365 dagar (kortbetalning)</p>
            <button
              className="pill-button"
              type="button"
              onClick={() => onSelectPlan('yearly')}
              disabled={isSubmitting}
            >
              Betala 499 kr med kort
            </button>
          </div>
        </div>
      )}

      {hasIntent && (
        <form className="payment-details" onSubmit={handleSubmit}>
          <h3>Betala med kort</h3>
          {formatAmount() && (
            <p className="auth-note">
              Du betalar <strong>{formatAmount()}</strong> för din prenumeration.
            </p>
          )}
          <div className="payment-card-element">
            <label>Kortuppgifter</label>
            <div ref={containerRef} className="stripe-payment-element" />
          </div>
          {localError && <p className="payment-error-text">{localError}</p>}
          {state.error && !localError && <p className="payment-error-text">{state.error}</p>}
          <button className="pill-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Bearbetar betalning…' : 'Bekräfta betalning'}
          </button>
        </form>
      )}

      {state.phase === 'confirmed' && (
        <div className="payment-success">
          <h3>✓ Kortbetalning genomförd!</h3>
          <p>
            Din betalning är genomförd. Din prenumeration uppdateras och blir aktiv inom kort.
          </p>
        </div>
      )}
    </>
  )
}
