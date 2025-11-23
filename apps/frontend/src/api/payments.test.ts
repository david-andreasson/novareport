import { describe, it, expect, vi, afterEach } from 'vitest'
import { createPayment, getPaymentStatus } from './payments'

const originalFetch = globalThis.fetch

afterEach(() => {
  globalThis.fetch = originalFetch
  vi.restoreAllMocks()
})

describe('payments API', () => {
  it('createPayment skickar POST med rätt body och returnerar payment-data', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        paymentId: 'p1',
        paymentAddress: 'address123',
        amountXmr: '0.01',
        expiresAt: '2024-01-01T00:00:00Z',
      }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const result = await createPayment('token-123', 'monthly', '0.01')

    expect(result.paymentId).toBe('p1')
    expect(fetchMock).toHaveBeenCalledWith('/api/payments/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-123',
      },
      body: JSON.stringify({
        plan: 'monthly',
        amountXmr: '0.01',
      }),
    })
  })

  it('createPayment kastar fel med backend-text vid misslyckande', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 500,
      json: async () => ({}),
      text: async () => 'Betalningen kunde inte skapas',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(createPayment('token-123', 'monthly', '0.01')).rejects.toThrow(
      'Betalningen kunde inte skapas',
    )
  })

  it('getPaymentStatus hämtar och returnerar status-data', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({
        paymentId: 'p1',
        status: 'PENDING',
        createdAt: '2024-01-01T00:00:00Z',
        confirmedAt: null,
      }),
      text: async () => '',
    }))

    ;(globalThis as any).fetch = fetchMock

    const result = await getPaymentStatus('token-123', 'p1')

    expect(result.status).toBe('PENDING')
    expect(fetchMock).toHaveBeenCalledWith('/api/payments/p1/status', {
      headers: { Authorization: 'Bearer token-123' },
    })
  })

  it('getPaymentStatus kastar fel med backend-text vid misslyckande', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      status: 404,
      json: async () => ({}),
      text: async () => 'Hittade inte betalning',
    }))

    ;(globalThis as any).fetch = fetchMock

    await expect(getPaymentStatus('token-123', 'p1')).rejects.toThrow('Hittade inte betalning')
  })
})
