export type DailyReport = {
  id?: string
  reportId?: string
  reportDate: string
  summary: string
  createdAt?: string
  updatedAt?: string
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

export async function getLatestReport(token: string): Promise<DailyReport | null> {
  const response = await fetch('/api/notifications/latest', {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (response.status === 404) {
    return null
  }

  if (response.status === 401) {
    throw new Error('Inloggningen har gått ut. Logga in igen.')
  }

  if (response.status === 403) {
    throw new Error('Du saknar prenumeration för att läsa rapporten.')
  }

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte hämta rapport')
    throw new Error(errorText)
  }

  return (await response.json()) as DailyReport
}
