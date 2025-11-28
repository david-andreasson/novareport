import type { UserProfile } from '../App'

async function extractErrorMessage(response: Response, defaultMessage: string): Promise<string> {

  const rawContentType =
    typeof (response as any).headers?.get === 'function'
      ? (response as any).headers.get('Content-Type')
      : null
  const contentType = rawContentType ?? ''

  try {
    if (contentType.includes('application/problem+json')) {
      const problem = (await response.json()) as
        | { title?: string; detail?: string; status?: number }
        | null
        | undefined
      if (problem && typeof problem === 'object') {
        const status = typeof problem.status === 'number' ? problem.status : undefined
        const title = typeof problem.title === 'string' ? problem.title : undefined
        const detail = typeof problem.detail === 'string' ? problem.detail : undefined

        if (status === 409 || title === 'EmailAlreadyExistsException') {
          return 'E-postadressen är redan registrerad. Logga in i stället.'
        }

        if (detail) return detail
        if (title) return title
      }
    } else {
      const text = await response.text()
      if (text) return text
    }
  } catch {
    // Ignorera parse-fel och fall tillbaka till standardmeddelandet
  }

  return defaultMessage
}

export async function login(email: string, password: string): Promise<{ accessToken: string }> {
  const response = await fetch('/api/accounts/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password,
    }),
  })

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Inloggning misslyckades')
    throw new Error(errorText)
  }

  return (await response.json()) as { accessToken: string }
}

export async function register(params: {
  email: string
  password: string
  firstName: string
  lastName: string
}): Promise<{ accessToken: string }> {
  const response = await fetch('/api/accounts/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  })

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Registrering misslyckades')
    throw new Error(errorText)
  }

  return (await response.json()) as { accessToken: string }
}

export async function getProfile(token: string): Promise<UserProfile> {
  const response = await fetch('/api/accounts/me', {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte hämta profil')
    throw new Error(errorText)
  }

  return (await response.json()) as UserProfile
}

export async function updateSettings(token: string, settings: {
  locale: string
  timezone: string
  marketingOptIn: boolean
  reportEmailOptIn: boolean
  twoFactorEnabled: boolean
}): Promise<void> {
  const response = await fetch('/api/accounts/me/settings', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(settings),
  })

  if (!response.ok) {
    const errorText = await extractErrorMessage(response, 'Kunde inte spara inställningar')
    throw new Error(errorText)
  }
}
