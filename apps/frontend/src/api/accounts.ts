import type { UserProfile } from '../App'

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
    const errorText = await response.text()
    throw new Error(errorText || 'Login failed')
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
    const errorText = await response.text()
    throw new Error(errorText || 'Registrering misslyckades')
  }

  return (await response.json()) as { accessToken: string }
}

export async function getProfile(token: string): Promise<UserProfile> {
  const response = await fetch('/api/accounts/me', {
    headers: { Authorization: `Bearer ${token}` },
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || 'Kunde inte hämta profil')
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
    const errorText = await response.text()
    throw new Error(errorText || 'Kunde inte spara inställningar')
  }
}
