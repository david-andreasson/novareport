export type DailyReport = {
  id?: string
  reportId?: string
  reportDate: string
  summary: string
  createdAt?: string
  updatedAt?: string
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
    const errorText = await response.text()
    throw new Error(errorText || 'Kunde inte hämta rapport')
  }

  return (await response.json()) as DailyReport
}
