export type CreatedPayment = {
  paymentId: string
  paymentAddress: string
  amountXmr: string
  expiresAt: string
}

export type PaymentStatus = {
  paymentId: string
  status: 'PENDING' | 'CONFIRMED' | 'FAILED'
  createdAt: string
  confirmedAt: string | null
}

async function extractErrorMessage(response: Response, defaultMessage: string): Promise<string> {
  const contentType = response.headers.get('Content-Type') ?? ''

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

export async function createPayment(
  token: string,
  plan: 'monthly' | 'yearly',
  amountXmr: string,
): Promise<CreatedPayment> {
  const response = await fetch('/api/payments/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      plan,
      amountXmr,
    }),
  })

  if (response.status === 401) {
    throw new Error('Inloggningen har gått ut. Logga in igen.')
  }

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte skapa betalning')
    throw new Error(errorText)
  }

  return (await response.json()) as CreatedPayment
}

export async function getPaymentStatus(
  token: string,
  paymentId: string,
): Promise<PaymentStatus> {
  const response = await fetch(`/api/payments/${paymentId}/status`, {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte hämta betalningsstatus')
    throw new Error(errorText)
  }

  return (await response.json()) as PaymentStatus
}
