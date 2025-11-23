import { describe, it, expect, vi, afterEach } from 'vitest'
import { getSubscriptionInfo } from './subscriptions'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('subscriptions API', () => {
  it('returnerar hasAccess=false och null-detail när användaren saknar access', async () => {
    const fetchMock = vi
      .fn()
      // /has-access
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ hasAccess: false }),
        text: async () => '',
      } as Response)

    ;(globalThis as any).fetch = fetchMock

    const result = await getSubscriptionInfo('token-123')

    expect(result).toEqual({ hasAccess: false, detail: null })
    expect(fetchMock).toHaveBeenCalledWith('/api/subscriptions/me/has-access', {
      headers: { Authorization: 'Bearer token-123' },
    })
  })

  it('hämtar detaljer när hasAccess=true', async () => {
    const fetchMock = vi
      .fn()
      // /has-access
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ hasAccess: true }),
        text: async () => '',
      } as Response)
      // /me
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          userId: 'user-1',
          plan: 'monthly',
          status: 'ACTIVE',
          startAt: '2024-01-01T00:00:00Z',
          endAt: '2024-02-01T00:00:00Z',
        }),
        text: async () => '',
      } as Response)

    ;(globalThis as any).fetch = fetchMock

    const result = await getSubscriptionInfo('token-123')

    expect(result.hasAccess).toBe(true)
    expect(result.detail?.plan).toBe('monthly')
  })

  it('kastar fel när has-access-anropet misslyckas', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 500,
      json: async () => ({}),
      text: async () => 'Serverfel',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(getSubscriptionInfo('token-123')).rejects.toThrow('Serverfel')
  })
})
