import { describe, it, expect, vi, afterEach } from 'vitest'
import { login, register, getProfile, updateSettings } from './accounts'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('accounts API', () => {
  it('login skickar POST med korrekt payload och returnerar accessToken', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({ accessToken: 'token-123' }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const result = await login('user@example.com', 'Password123!')

    expect(result).toEqual({ accessToken: 'token-123' })
    expect(fetchMock).toHaveBeenCalledWith('/api/accounts/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'user@example.com', password: 'Password123!' }),
    })
  })

  it('login kastar fel med text från backend vid misslyckande', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 401,
      json: async () => ({}),
      text: async () => 'Felaktiga uppgifter',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(login('user@example.com', 'wrong')).rejects.toThrow('Felaktiga uppgifter')
  })

  it('register skickar POST med korrekt payload och returnerar accessToken', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({ accessToken: 'token-xyz' }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const params = {
      email: 'new@example.com',
      password: 'Password123!',
      firstName: 'Anna',
      lastName: 'Svensson',
    }

    const result = await register(params)

    expect(result).toEqual({ accessToken: 'token-xyz' })
    expect(fetchMock).toHaveBeenCalledWith('/api/accounts/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    })
  })

  it('getProfile hämtar profil med bearer-token', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        id: 'user-1',
        email: 'user@example.com',
        firstName: 'Anna',
        lastName: 'Svensson',
        role: 'USER',
      }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const profile = await getProfile('token-123')

    expect(profile.email).toBe('user@example.com')
    expect(fetchMock).toHaveBeenCalledWith('/api/accounts/me', {
      headers: { Authorization: 'Bearer token-123' },
    })
  })

  it('updateSettings skickar PUT med rätt body och kastar fel vid misslyckande', async () => {
    const okResponse = {
      ok: true,
      status: 200,
      json: async () => ({}),
      text: async () => '',
    }

    const errorResponse = {
      ok: false,
      status: 400,
      json: async () => ({}),
      text: async () => 'Ogiltiga inställningar',
    }

    const fetchMock = vi
      .fn()
      // Första anropet: OK
      .mockResolvedValueOnce(okResponse as Response)
      // Andra anropet: fel
      .mockResolvedValueOnce(errorResponse as Response)

    ;(globalThis as any).fetch = fetchMock

    const payload = {
      locale: 'en-US',
      timezone: 'Europe/London',
      marketingOptIn: true,
      reportEmailOptIn: true,
      twoFactorEnabled: false,
    }

    await updateSettings('token-123', payload)
    expect(fetchMock).toHaveBeenCalledWith('/api/accounts/me/settings', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-123',
      },
      body: JSON.stringify(payload),
    })

    await expect(updateSettings('token-123', payload)).rejects.toThrow('Ogiltiga inställningar')
  })
})
