import { describe, it, expect, vi, afterEach } from 'vitest'
import { getLatestReport } from './reports'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('reports API', () => {
  it('returns null on 404 (no report)', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 404,
      json: async () => ({}),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const result = await getLatestReport('token-123')
    expect(result).toBeNull()
  })

  it('throws error with correct message on 401 (logged out)', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 401,
      json: async () => ({}),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(getLatestReport('token-123')).rejects.toThrow(
      'Inloggningen har gått ut. Logga in igen.',
    )
  })

  it('throws error with correct message on 403 (no subscription)', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 403,
      json: async () => ({}),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(getLatestReport('token-123')).rejects.toThrow(
      'Du saknar prenumeration för att läsa rapporten.',
    )
  })

  it('returns report on successful response', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        reportDate: '2024-01-01T00:00:00Z',
        summary: 'Sammanfattning',
      }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const report = await getLatestReport('token-123')
    expect(report?.summary).toBe('Sammanfattning')
  })
})
