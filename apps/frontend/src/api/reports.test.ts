import { describe, it, expect, vi, afterEach } from 'vitest'
import { getLatestReport } from './reports'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('reports API', () => {
  it('returnerar null vid 404 (ingen rapport)', async () => {
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

  it('kastar fel med rätt meddelande vid 401 (utloggad)', async () => {
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

  it('kastar fel med rätt meddelande vid 403 (saknar prenumeration)', async () => {
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

  it('returnerar rapport vid lyckat svar', async () => {
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
