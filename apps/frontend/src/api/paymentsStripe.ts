export type StripeCreatedPayment = {
  paymentId: string
  clientSecret: string
  amountFiat: number
  currencyFiat: string
}

async function extractErrorMessage(response: Response, defaultMessage: string): Promise<string> {
  const rawContentType =
    typeof (response as any).headers?.get === 'function'
      ? (response as any).headers.get('Content-Type')
      : null
  const contentType = rawContentType ?? ''

  try {
    if (contentType.includes('application/problem+json')) {
      const problem = (await response.json()) as { title?: string; detail?: string } | null | undefined
      if (problem && typeof problem === 'object') {
        if (typeof problem.detail === 'string') return problem.detail
        if (typeof problem.title === 'string') return problem.title
      }
    } else {
      const text = await response.text()
      if (text) return text
    }
  } catch {
    // Ignorera parse-fel
  }

  return defaultMessage
}

export async function createStripePaymentIntent(
  token: string,
  plan: 'monthly' | 'yearly',
): Promise<StripeCreatedPayment> {
  const response = await fetch('/api/payments-stripe/create-intent', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ plan }),
  })

  if (response.status === 401) {
    throw new Error('Inloggningen har gått ut. Logga in igen.')
  }

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte skapa Stripe-betalning')
    throw new Error(errorText)
  }

  return (await response.json()) as StripeCreatedPayment
}
