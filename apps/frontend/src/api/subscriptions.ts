export type SubscriptionDetail = {
  userId: string
  plan: string
  status: string
  startAt: string
  endAt: string
}

export type SubscriptionInfoResult = {
  hasAccess: boolean
  detail: SubscriptionDetail | null
}

export async function getSubscriptionInfo(token: string): Promise<SubscriptionInfoResult> {
  const accessResponse = await fetch('/api/subscriptions/me/has-access', {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (accessResponse.status === 401) {
    throw new Error('Inloggningen har gått ut. Logga in igen.')
  }

  if (!accessResponse.ok) {
    const errorText = await accessResponse.text()
    throw new Error(errorText || 'Kunde inte kontrollera prenumeration')
  }

  const { hasAccess } = (await accessResponse.json()) as { hasAccess: boolean }
  let detail: SubscriptionDetail | null = null

  if (hasAccess) {
    const detailResponse = await fetch('/api/subscriptions/me', {
      headers: { Authorization: `Bearer ${token}` },
    })

    if (detailResponse.ok) {
      detail = (await detailResponse.json()) as SubscriptionDetail
    } else if (detailResponse.status !== 404) {
      const errorText = await detailResponse.text()
      throw new Error(errorText || 'Kunde inte hämta prenumerationsdetaljer')
    }
  }

  return { hasAccess, detail }
}
