import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import App, {
  translateStatus,
  renderInlineMarkdown,
  renderReportSummary,
} from './App'

describe('translateStatus', () => {
  it('översätter kända statuser till svenska', () => {
    expect(translateStatus('ACTIVE')).toBe('Aktiv')
    expect(translateStatus('EXPIRED')).toBe('Utgången')
    expect(translateStatus('CANCELLED')).toBe('Avslutad')
  })

  it('lämnar okända statuser oförändrade', () => {
    expect(translateStatus('UNKNOWN')).toBe('UNKNOWN')
  })
})

describe('renderInlineMarkdown', () => {
  it('wrappar **text** i <strong>', () => {
    const { container } = render(<>{renderInlineMarkdown('Hello **World**')}</>)
    const strong = container.querySelector('strong')
    expect(strong).not.toBeNull()
    expect(strong?.textContent).toBe('World')
  })
})

describe('renderReportSummary', () => {
  it('renderar fallback-text när sammanfattningen är tom', () => {
    const { container } = render(<>{renderReportSummary('')}</>)
    const empty = container.querySelector('.report-summary__empty')
    expect(empty).not.toBeNull()
  })

  it('renderar rubriker för markdown-överskrifter', () => {
    const summary = '# Titel\n\n- Punkt 1\n- Punkt 2'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const heading = container.querySelector('.report-summary__heading')
    expect(heading).not.toBeNull()
    expect(heading?.textContent).toContain('Titel')
  })

  it('renderar punktlista som &lt;ul&gt; med &lt;li&gt;-element', () => {
    const summary = '- Första punkt\n- Andra punkt'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const list = container.querySelector('.report-summary__list')
    expect(list).not.toBeNull()
    const items = list?.querySelectorAll('li') ?? []
    expect(items.length).toBe(2)
  })

  it('behandlar en bold-rad som rubrik och rensar numerisk prefix', () => {
    const summary = '**1. Viktig rubrik**\n\nText under rubrik.'
    const { container } = render(<>{renderReportSummary(summary)}</>)
    const heading = container.querySelector('.report-summary__heading--h2')
    expect(heading).not.toBeNull()
    expect(heading?.textContent).toBe('Viktig rubrik')
  })

  it('delar upp text i flera stycken vid tomrader', () => {
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

  it('kan logga in och visa profilvy med prenumerationsstatus', async () => {
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
    await screen.findByText('user@example.com')
    await screen.findByText('Prenumerationsstatus')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/auth/login'),
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('visar felmeddelande vid misslyckad inloggning', async () => {
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

  it('visar rätt felmeddelanden för rapport och prenumeration när användaren inte är inloggad', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Rapport' }))
    await screen.findByText('Logga in för att se rapporten.')
    await screen.findByText('Logga in för att kunna läsa rapporten.')

    fireEvent.click(screen.getByRole('button', { name: 'Prenumerera' }))
    await screen.findByText('Logga in för att prenumerera.')
  })

  it('sparar inställningar när användaren är inloggad', async () => {
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
        expect(parsed.locale).toBe('en-US')
        expect(parsed.timezone).toBe('Europe/London')
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

    // Logga in först
    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    // Gå till inställningar
    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    const localeInput = await screen.findByLabelText('Språk (locale)')
    const timezoneInput = screen.getByLabelText('Tidszon')
    const marketingCheckbox = screen.getByLabelText(
      'Ta emot nyheter och uppdateringar',
    ) as HTMLInputElement
    const reportCheckbox = screen.getByLabelText(
      'Ta emot daglig rapport via e-post',
    ) as HTMLInputElement

    fireEvent.change(localeInput, { target: { value: 'en-US' } })
    fireEvent.change(timezoneInput, { target: { value: 'Europe/London' } })
    fireEvent.click(marketingCheckbox)
    fireEvent.click(reportCheckbox)

    fireEvent.click(screen.getByRole('button', { name: 'Spara inställningar' }))

    await screen.findByText('Inställningar sparade')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/me/settings'),
      expect.any(Object),
    )
  })

  it('visar felmeddelande när man öppnar profil utan att vara inloggad', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Min profil' }))

    await screen.findByText('Logga in för att se din profil.')
  })

  it('visar felmeddelande när man öppnar inställningar utan att vara inloggad', async () => {
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    await screen.findByText('Logga in för att ändra inställningar.')
  })

  it('kan skapa konto och visa profilvy efter registrering', async () => {
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
    await screen.findByText('new@example.com')

    expect(fetchMock).toHaveBeenCalled()
  })

  it('visar senaste rapport när användaren är inloggad och har prenumeration', async () => {
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
  it('visar felmeddelande när sparande av inställningar misslyckas', async () => {
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

    // Logga in först
    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Lösenord'), {
      target: { value: 'Password123!' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Fortsätt' }))

    await screen.findByText('Inloggning lyckades')

    // Gå till inställningar
    fireEvent.click(screen.getByRole('button', { name: 'Inställningar' }))

    const localeInput = await screen.findByLabelText('Språk (locale)')
    const timezoneInput = screen.getByLabelText('Tidszon')

    fireEvent.change(localeInput, { target: { value: 'en-US' } })
    fireEvent.change(timezoneInput, { target: { value: 'Europe/London' } })

    fireEvent.click(screen.getByRole('button', { name: 'Spara inställningar' }))

    await screen.findByText('Kunde inte spara inställningar')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/accounts/me/settings'),
      expect.any(Object),
    )
  })

  it('visar felmeddelande när det inte finns någon rapport ännu', async () => {
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

  it('visar felmeddelande när rapport inte kan hämtas på grund av saknad prenumeration', async () => {
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

  it('kan navigera från Skapa konto tillbaka till Logga in via sidomenyn', async () => {
    render(<App />)

    // Gå till registreringsvyn
    fireEvent.click(screen.getByRole('button', { name: 'Skapa konto' }))
    await screen.findByRole('heading', { name: 'Skapa konto' })

    // Navigera tillbaka till login
    fireEvent.click(screen.getByRole('button', { name: 'Logga in' }))
    await screen.findByRole('heading', { name: 'Logga in' })
  })
})
