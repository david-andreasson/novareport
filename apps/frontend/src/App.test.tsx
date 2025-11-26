import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import App, {
  translateStatus,
  renderInlineMarkdown,
  renderReportSummary,
} from './App'

describe('translateStatus', () => {
  it('translates known statuses to Swedish', () => {
    expect(translateStatus('ACTIVE')).toBe('Aktiv')
    expect(translateStatus('EXPIRED')).toBe('Utgången')
    expect(translateStatus('CANCELLED')).toBe('Avslutad')
  })

  it('leaves unknown statuses unchanged', () => {
    expect(translateStatus('UNKNOWN')).toBe('UNKNOWN')
  })
})

describe('renderInlineMarkdown', () => {
  it('wraps **text** in <strong>', () => {
    const { container } = render(<>{renderInlineMarkdown('Hello **World**')}</>)
    const strong = container.querySelector('strong')
    expect(strong).not.toBeNull()
    expect(strong?.textContent).toBe('World')
  })
})

describe('renderReportSummary', () => {
  it('renders fallback text when summary is empty', () => {
    const { container } = render(<>{renderReportSummary('')}</>)
    const empty = container.querySelector('.report-summary__empty')
    expect(empty).not.toBeNull()
  })

  it('renders headings for markdown titles', () => {
    const summary = '# Titel\n\n- Punkt 1\n- Punkt 2'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const heading = container.querySelector('.report-summary__heading')
    expect(heading).not.toBeNull()
    expect(heading?.textContent).toContain('Titel')
  })

  it('renders bullet list as <ul> with <li> elements', () => {
    const summary = '- Första punkt\n- Andra punkt'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const list = container.querySelector('.report-summary__list')
    expect(list).not.toBeNull()
    const items = list?.querySelectorAll('li') ?? []
    expect(items.length).toBe(2)
  })

  it('treats a bold-only line as a heading and strips numeric prefix', () => {
    const summary = '**1. Viktig rubrik**\n\nText under rubrik.'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const heading = container.querySelector('.report-summary__heading--h2')
    expect(heading).not.toBeNull()
    expect(heading?.textContent).toBe('Viktig rubrik')
  })

  it('splits text into multiple paragraphs on blank lines', () => {
    const summary = 'Första stycket.\n\nAndra stycket.'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const paragraphs = container.querySelectorAll('.report-summary__paragraph')
    expect(paragraphs.length).toBe(2)
  })
})

describe('App integration', () => {
  const setupFetchMock = (
    impl: (url: string, init?: RequestInit) => Promise<Response>,
  ) => {
    const fetchMock = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : input.toString()
      return impl(url, init)
    })

    ;(globalThis as any).fetch = fetchMock
    return fetchMock
  }

  it('can log in and show profile view with subscription status', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: true }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
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
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')
    await screen.findByRole('heading', { name: 'Min profil' })
    await screen.findByText('Anna Svensson')
    await screen.findAllByText('user@example.com')
    await screen.findByText('Prenumerationsstatus')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/auth/login'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('shows error message on failed login', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: false,
          status: 401,
          json: async () => ({}),
          text: async () => 'Felaktiga uppgifter',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'wrong' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Felaktiga uppgifter')

    expect(fetchMock).toHaveBeenCalled()
  })

  it('shows correct error messages for report and subscription when user is not logged in', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Rapport' }))
    await screen.findByText('Logga in för att se rapporten.')
    await screen.findByText('Logga in för att kunna läsa rapporten.')

    fireEvent.click(screen.getByRole('button', { name: 'Prenumerera' }))
    await screen.findByText('Logga in för att prenumerera.')
  })

  it('saves settings when user is logged in', async () => {
    const fetchMock = setupFetchMock(async (url, init) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: false }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me/settings')) {
        const method = (init && init.method) || 'GET'
        expect(method).toBe('PUT')

        const body = init?.body as string
        const parsed = JSON.parse(body)
        expect(parsed.locale).toBe('sv-SE')
        expect(parsed.timezone).toBe('Europe/Stockholm')
        expect(parsed.marketingOptIn).toBe(true)
        expect(parsed.reportEmailOptIn).toBe(true)
        expect(parsed.twoFactorEnabled).toBe(false)

        return {
          ok: true,
          status: 200,
          json: async () => ({}),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    // Log in first
    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    // Go to settings
    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    const marketingCheckbox = (await screen.findByLabelText(
      'Ta emot nyheter och uppdateringar',
    )) as HTMLInputElement
    const reportCheckbox = screen.getByLabelText(
      'Ta emot daglig rapport via e-post',
    ) as HTMLInputElement
    fireEvent.click(marketingCheckbox)
    fireEvent.click(reportCheckbox)

    fireEvent.click(screen.getByRole('button', { name: 'Spara inställningar' }))

    await screen.findByText('Inställningar sparade')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/me/settings'),
      expect.any(Object),
    )
  })

  it('shows error message when opening profile without being logged in', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Min profil' }))

    await screen.findByText('Logga in för att se din profil.')
  })

  it('shows error message when opening settings without being logged in', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    await screen.findByText('Logga in för att ändra inställningar.')
  })

  it('can create account and show profile view after registration', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/register')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-xyz' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            id: 'user-2',
            email: 'new@example.com',
            firstName: 'Anna',
            lastName: 'Svensson',
            role: 'USER',
          }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: false }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
          ok: false,
          status: 404,
          json: async () => ({}),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Skapa konto' }))

    fireEvent.change(screen.getByLabelText('Förnamn'), {
      target: { value: 'Anna' },
    })
    fireEvent.change(screen.getByLabelText('Efternamn'), {
      target: { value: 'Svensson' },
    })
    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'new@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.change(screen.getByLabelText('Bekräfta lösenord'), {
      target: { value: 'Password123!' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Registrera' }))

    await screen.findByText('Konto skapat och inloggad')
    await screen.findByRole('heading', { name: 'Min profil' })
    await screen.findByText('Anna Svensson')
    await screen.findAllByText('new@example.com')

    expect(fetchMock).toHaveBeenCalled()
  })

  it('shows latest report when user is logged in and has subscription', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: true }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/notifications/latest')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            reportDate: '2024-01-01T00:00:00Z',
            summary: 'Dagens rapport: Allt ser bra ut.',
          }),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    fireEvent.click(screen.getByRole('button', { name: 'Rapport' }))

    await screen.findByText('Senaste rapport')
    await screen.findByText('Dagens rapport: Allt ser bra ut.')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications/latest'),
      expect.any(Object),
    )
  })
  it('shows error message when saving settings fails', async () => {
    const fetchMock = setupFetchMock(async (url, init) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: false }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me/settings')) {
        const method = (init && init.method) || 'GET'
        expect(method).toBe('PUT')

        return {
          ok: false,
          status: 400,
          json: async () => ({}),
          text: async () => 'Kunde inte spara inställningar',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    // Log in first
    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    // Go to settings
    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    fireEvent.click(screen.getByRole('button', { name: 'Spara inställningar' }))

    await screen.findByText('Kunde inte spara inställningar')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/me/settings'),
      expect.any(Object),
    )
  })

  it('shows error message when there is no report yet', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: true }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/notifications/latest')) {
        return {
          ok: false,
          status: 404,
          json: async () => ({}),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    fireEvent.click(screen.getByRole('button', { name: 'Rapport' }))

    await screen.findByText('Ingen rapport tillgänglig ännu.')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications/latest'),
      expect.any(Object),
    )
  })

  it('shows error message when report cannot be fetched due to missing subscription', async () => {
    const fetchMock = setupFetchMock(async (url) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: true }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/notifications/latest')) {
        return {
          ok: false,
          status: 403,
          json: async () => ({}),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    fireEvent.click(screen.getByRole('button', { name: 'Rapport' }))

    await screen.findByText('Du saknar prenumeration för att läsa rapporten.')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/notifications/latest'),
      expect.any(Object),
    )
  })

  it('can navigate from Create account back to Login via the sidebar', async () => {
    render(<App />)

    // Go to registration view
    fireEvent.click(screen.getByRole('button', { name: 'Skapa konto' }))
    await screen.findAllByRole('heading', { name: 'Skapa konto' })

    // Navigate back to login
    fireEvent.click(screen.getByRole('button', { name: 'Logga in' }))
    await screen.findAllByRole('heading', { name: 'Logga in' })
  })

  it('can initiate payment after login and show payment details', async () => {
    const fetchMock = setupFetchMock(async (url, _init) => {
      if (url.endsWith('/api/accounts/auth/login')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ accessToken: 'token-123' }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/accounts/me')) {
        return {
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
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me/has-access')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({ hasAccess: false }),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/subscriptions/me')) {
        return {
          ok: false,
          status: 404,
          json: async () => ({}),
          text: async () => '',
        } as Response
      }

      if (url.endsWith('/api/payments/create')) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            paymentId: 'payment-1',
            paymentAddress: 'monero-address-123',
            amountXmr: '0.01',
            expiresAt: '2024-01-01T00:00:00Z',
          }),
          text: async () => '',
        } as Response
      }

      return {
        ok: false,
        status: 404,
        json: async () => ({}),
        text: async () => '',
      } as Response
    })

    render(<App />)

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    fireEvent.click(screen.getByRole('button', { name: 'Prenumerera' }))
    await screen.findByRole('heading', { name: 'Prenumerera' })

    fireEvent.click(screen.getByRole('button', { name: 'Välj månad' }))

    await screen.findByText('Skicka betalning')
    await screen.findByText('0.01 XMR')
    await screen.findByText('monero-address-123')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/payments/create'),
      expect.objectContaining({ method: 'POST' }),
    )
  })
})
