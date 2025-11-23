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
    const errorText = await response.text()
    throw new Error(errorText || 'Kunde inte skapa betalning')
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
    const errorText = await response.text()
    throw new Error(errorText || 'Kunde inte hämta betalningsstatus')
  }

  return (await response.json()) as PaymentStatus
}
