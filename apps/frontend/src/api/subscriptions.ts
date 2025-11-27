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

export async function getSubscriptionInfo(token: string): Promise<SubscriptionInfoResult> {
  const accessResponse = await fetch('/api/subscriptions/me/has-access', {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (accessResponse.status === 401) {
    throw new Error('Inloggningen har gått ut. Logga in igen.')
  }

  if (!accessResponse.ok) {
    const errorText = await extractErrorMessage(accessResponse, 'Kunde inte kontrollera prenumeration')
    throw new Error(errorText)
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
      const errorText = await extractErrorMessage(detailResponse, 'Kunde inte hämta prenumerationsdetaljer')
      throw new Error(errorText)
    }
  }

  return { hasAccess, detail }
}
