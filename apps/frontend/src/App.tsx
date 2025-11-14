import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent, JSX } from 'react'
import './App.css'

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

type View = 'login' | 'register' | 'profile' | 'settings' | 'report' | 'subscribe'

type Message = {
  scope: View
  status: 'success' | 'error'
  text: string
}

type UserProfile = {
  id: string
  email: string
  firstName: string
  lastName: string
  role: string
}

type SubscriptionDetail = {
  userId: string
  plan: string
  status: string
  startAt: string
  endAt: string
}

type SubscriptionState = {
  phase: 'idle' | 'loading' | 'success' | 'error'
  hasAccess: boolean | null
  detail: SubscriptionDetail | null
  error?: string
}

type DailyReport = {
  id?: string
  reportId?: string
  reportDate: string
  summary: string
  createdAt?: string
  updatedAt?: string
}

type LatestReportState = {
  phase: 'idle' | 'loading' | 'success' | 'error'
  report: DailyReport | null
  error?: string
}

type PaymentInfo = {
  paymentId: string
  paymentAddress: string
  amountXmr: string
  expiresAt: string
}

type PaymentState = {
  phase: 'idle' | 'selecting' | 'pending' | 'polling' | 'confirmed' | 'expired' | 'error'
  selectedPlan: 'monthly' | 'yearly' | null
  payment: PaymentInfo | null
  error?: string
}

const translateStatus = (status: string) => {
  switch (status) {
    case 'ACTIVE':
      return 'Aktiv'
    case 'EXPIRED':
      return 'Utgången'
    case 'CANCELLED':
      return 'Avslutad'
    default:
      return status
  }
}

const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('sv-SE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })

const formatReportTimestamp = (report: DailyReport) => {
  const iso = report.updatedAt ?? report.createdAt ?? report.reportDate
  return formatDateTime(iso)
}

function App() {
  const [view, setView] = useState<View>('login')
  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const createInitialRegisterForm = () => ({
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
  })
  const [registerForm, setRegisterForm] = useState(createInitialRegisterForm())
  const [token, setToken] = useState<string | null>(null)
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [settingsForm, setSettingsForm] = useState({
    locale: 'sv-SE',
    timezone: 'Europe/Stockholm',
    marketingOptIn: false,
  })
  const [message, setMessage] = useState<Message | null>(null)
  const [loading, setLoading] = useState<View | null>(null)
  const [subscriptionState, setSubscriptionState] = useState<SubscriptionState>({
    phase: 'idle',
    hasAccess: null,
    detail: null,
  })
  const [reportState, setReportState] = useState<LatestReportState>({
    phase: 'idle',
    report: null,
  })
  const [paymentState, setPaymentState] = useState<PaymentState>({
    phase: 'idle',
    selectedPlan: null,
    payment: null,
  })

  const passwordPolicyRegex = /^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/
  const meetsPasswordPolicy = passwordPolicyRegex.test(registerForm.password)
  const passwordsMatch =
    registerForm.password.length > 0 &&
    registerForm.confirmPassword.length > 0 &&
    registerForm.password === registerForm.confirmPassword
  const showPasswordFeedback =
    registerForm.password.length > 0 || registerForm.confirmPassword.length > 0

  const renderMessage = (scope: View) =>
    message?.scope === scope ? (
      <p className={`auth-feedback ${message.status}`}>{message.text}</p>
    ) : null

  const handleChangeView = (nextView: View) => {
    setView(nextView)
    setMessage((prev) => (prev?.scope === nextView ? prev : null))
  }

  const fetchSubscriptionInfo = async () => {
    if (!token) {
      setSubscriptionState({ phase: 'idle', hasAccess: null, detail: null })
      return
    }
    setSubscriptionState((prev) => ({ ...prev, phase: 'loading', error: undefined }))
    try {
      const accessResponse = await fetch('/api/subscriptions/me/has-access', {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (accessResponse.status === 401) {
        throw new Error('Inloggningen har gått ut. Logga in igen.')
      }

      if (!accessResponse.ok) {
        const errorText = await accessResponse.text()
        throw new Error(errorText || 'Kunde inte kontrollera prenumeration')
      }

      const { hasAccess } = (await accessResponse.json()) as { hasAccess: boolean }
      let detail: SubscriptionDetail | null = null

      if (hasAccess) {
        const detailResponse = await fetch('/api/subscriptions/me', {
          headers: { Authorization: `Bearer ${token}` },
        })

        if (detailResponse.ok) {
          detail = (await detailResponse.json()) as SubscriptionDetail
        } else if (detailResponse.status !== 404) {
          const errorText = await detailResponse.text()
          throw new Error(errorText || 'Kunde inte hämta prenumerationsdetaljer')
        }
      }

      setSubscriptionState({ phase: 'success', hasAccess, detail })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setSubscriptionState({ phase: 'error', hasAccess: null, detail: null, error: text })
    }
  }

  const getLatestReport = async (): Promise<DailyReport | null> => {
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


  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading('login')
    setMessage((prev) => (prev?.scope === 'login' ? null : prev))
    try {
      const response = await fetch('/api/accounts/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: loginForm.email,
          password: loginForm.password,
        }),
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Login failed')
      }

      const data: { accessToken: string } = await response.json()
      setToken(data.accessToken)
      setProfile(null)
      setSubscriptionState({ phase: 'idle', hasAccess: null, detail: null })
      setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
      setMessage({ scope: 'profile', status: 'success', text: 'Inloggning lyckades' })
      setView('profile')
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setMessage({ scope: 'login', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const handleRegisterSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!meetsPasswordPolicy || !passwordsMatch) {
      setMessage({
        scope: 'register',
        status: 'error',
        text: 'Kontrollera att lösenordet uppfyller kraven och att fälten matchar.',
      })
      return
    }
    setLoading('register')
    setMessage((prev) => (prev?.scope === 'register' ? null : prev))
    try {
      const response = await fetch('/api/accounts/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: registerForm.email,
          password: registerForm.password,
          firstName: registerForm.firstName,
          lastName: registerForm.lastName,
        }),
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Registrering misslyckades')
      }

      const data: { accessToken: string } = await response.json()
      setToken(data.accessToken)
      setProfile(null)
      setSubscriptionState({ phase: 'idle', hasAccess: null, detail: null })
      setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
      setMessage({ scope: 'profile', status: 'success', text: 'Konto skapat och inloggad' })
      setView('profile')
      setRegisterForm(createInitialRegisterForm())
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setMessage({ scope: 'register', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const fetchProfile = async () => {
    if (!token) {
      setProfile(null)
      return
    }
    setLoading('profile')
    setMessage((prev) => (prev?.scope === 'profile' ? null : prev))
    try {
      const response = await fetch('/api/accounts/me', {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Kunde inte hämta profil')
      }

      const data: UserProfile = await response.json()
      setProfile(data)
      setMessage({ scope: 'profile', status: 'success', text: 'Profil hämtad' })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setProfile(null)
      setMessage({ scope: 'profile', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const handleSettingsInput = (field: 'locale' | 'timezone') =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const { value } = event.target
      setSettingsForm((prev) => ({ ...prev, [field]: value }))
    }

  const handleSettingsCheckbox = (field: 'marketingOptIn') =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const { checked } = event.target
      setSettingsForm((prev) => ({ ...prev, [field]: checked }))
    }

  const handleSettingsSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) {
      setMessage({ scope: 'settings', status: 'error', text: 'Logga in innan du sparar inställningar.' })
      return
    }

    setLoading('settings')
    setMessage((prev) => (prev?.scope === 'settings' ? null : prev))
    try {
      const response = await fetch('/api/accounts/me/settings', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ ...settingsForm, twoFactorEnabled: false }),
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Kunde inte spara inställningar')
      }

      setMessage({ scope: 'settings', status: 'success', text: 'Inställningar sparade' })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setMessage({ scope: 'settings', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const handleRefreshProfile = () => {
    void fetchProfile()
    void fetchSubscriptionInfo()
  }

  const fetchLatestReport = async () => {
    if (!token) {
      setReportState({ phase: 'error', report: null, error: 'Logga in för att se rapporten.' })
      return
    }

    setReportState({ phase: 'loading', report: null })
    try {
      const data = await getLatestReport()
      if (!data) {
        setReportState({ phase: 'error', report: null, error: 'Ingen rapport tillgänglig ännu.' })
        return
      }
      setReportState({ phase: 'success', report: data })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setReportState({ phase: 'error', report: null, error: text })
    }
  }

  const renderReportSummary = (summary: string) => {
    const lines = summary
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)

    if (lines.length === 0) {
      return <p className="report-summary__empty">Ingen sammanfattning tillgänglig.</p>
    }

    const lead = lines[0].startsWith('- ') ? null : lines[0]
    const remainder = lead ? lines.slice(1) : lines
    const bullets = remainder
      .filter((line) => line.startsWith('- '))
      .map((line) => line.replace(/^-\s*/, ''))
    const paragraphs = remainder.filter((line) => !line.startsWith('- '))

    return (
      <>
        {lead && <p className="report-summary__lead">{lead}</p>}
        {bullets.length > 0 && (
          <ul className="report-summary__list">
            {bullets.map((item, index) => (
              <li key={index}>{item}</li>
            ))}
          </ul>
        )}
        {paragraphs.map((text, index) => (
          <p key={`p-${index}`} className="report-summary__paragraph">
            {text}
          </p>
        ))}
      </>
    )
  }

  const handleSelectPlan = async (plan: 'monthly' | 'yearly') => {
    if (!token) {
      setMessage({ scope: 'subscribe', status: 'error', text: 'Logga in för att prenumerera.' })
      return
    }

    setPaymentState({ phase: 'selecting', selectedPlan: plan, payment: null })
    setMessage(null)

    try {
      const amountXmr = plan === 'monthly' ? '0.05' : '0.50'
      const response = await fetch('/api/payments/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          plan,
          amountXmr,
        }),
      })

      if (response.status === 401) {
        throw new Error('Inloggningen har gått ut. Logga in igen.')
      }

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Kunde inte skapa betalning')
      }

      const data: {
        paymentId: string
        paymentAddress: string
        amountXmr: string
        expiresAt: string
      } = await response.json()

      setPaymentState({
        phase: 'pending',
        selectedPlan: plan,
        payment: {
          paymentId: data.paymentId,
          paymentAddress: data.paymentAddress,
          amountXmr: data.amountXmr,
          expiresAt: data.expiresAt,
        },
      })

      // Start polling for payment status
      void pollPaymentStatus(data.paymentId)
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setPaymentState({ phase: 'error', selectedPlan: null, payment: null, error: text })
    }
  }

  const pollPaymentStatus = async (paymentId: string) => {
    if (!token) return

    setPaymentState((prev) => ({ ...prev, phase: 'polling' }))

    const maxAttempts = 120 // 10 minutes (5 seconds * 120)
    let attempts = 0

    while (attempts < maxAttempts) {
      try {
        await delay(5000) // Wait 5 seconds between polls

        const response = await fetch(`/api/payments/${paymentId}/status`, {
          headers: { Authorization: `Bearer ${token}` },
        })

        if (!response.ok) {
          throw new Error('Kunde inte hämta betalningsstatus')
        }

        const data: {
          paymentId: string
          status: 'PENDING' | 'CONFIRMED' | 'FAILED'
          createdAt: string
          confirmedAt: string | null
        } = await response.json()

        if (data.status === 'CONFIRMED') {
          setPaymentState((prev) => ({ ...prev, phase: 'confirmed' }))
          setMessage({
            scope: 'subscribe',
            status: 'success',
            text: 'Betalning bekräftad! Din prenumeration är nu aktiv.',
          })
          // Refresh subscription info
          void fetchSubscriptionInfo()
          return
        }

        if (data.status === 'FAILED') {
          setPaymentState((prev) => ({
            ...prev,
            phase: 'error',
            error: 'Betalningen misslyckades',
          }))
          return
        }

        attempts++
      } catch (error) {
        console.error('Poll error:', error)
        attempts++
      }
    }

    // Timeout after max attempts
    setPaymentState((prev) => ({ ...prev, phase: 'expired' }))
  }

  useEffect(() => {
    if (view === 'profile') {
      if (!token) {
        setProfile(null)
        setSubscriptionState({ phase: 'idle', hasAccess: null, detail: null })
        setMessage({ scope: 'profile', status: 'error', text: 'Logga in för att se profilen.' })
        return
      }
      void fetchProfile()
      void fetchSubscriptionInfo()
    } else if (view === 'settings' && !token) {
      setMessage({ scope: 'settings', status: 'error', text: 'Logga in för att ändra inställningar.' })
    } else if (view === 'report') {
      void fetchLatestReport()
    } else if (view === 'subscribe') {
      // Reset payment state when entering subscribe view
      setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
      if (!token) {
        setMessage({ scope: 'subscribe', status: 'error', text: 'Logga in för att prenumerera.' })
      }
    }
  }, [view, token])

  let panelContent: JSX.Element

  switch (view) {
    case 'login':
      panelContent = (
        <>
          <h2>Logga in</h2>
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <label>
              E-post
              <input
                type="email"
                name="loginEmail"
                placeholder="namn@example.com"
                value={loginForm.email}
                onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
                required
              />
            </label>
            <label>
              Lösenord
              <input
                type="password"
                name="loginPassword"
                placeholder="••••••"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm({ ...loginForm, password: event.target.value })
                }
                required
              />
            </label>
            {renderMessage('login')}
            <button className="pill-button" type="submit" disabled={loading === 'login'}>
              {loading === 'login' ? 'Arbetar…' : 'Fortsätt'}
            </button>
          </form>
        </>
      )
      break
    case 'register':
      panelContent = (
        <>
          <h2>Skapa konto</h2>
          <form className="auth-form" onSubmit={handleRegisterSubmit}>
            <label>
              Förnamn
              <input
                type="text"
                name="registerFirstName"
                placeholder="Anna"
                value={registerForm.firstName}
                onChange={(event) =>
                  setRegisterForm({ ...registerForm, firstName: event.target.value })
                }
                required
              />
            </label>
            <label>
              Efternamn
              <input
                type="text"
                name="registerLastName"
                placeholder="Svensson"
                value={registerForm.lastName}
                onChange={(event) =>
                  setRegisterForm({ ...registerForm, lastName: event.target.value })
                }
                required
              />
            </label>
            <label>
              E-post
              <input
                type="email"
                name="registerEmail"
                placeholder="namn@example.com"
                value={registerForm.email}
                onChange={(event) =>
                  setRegisterForm({ ...registerForm, email: event.target.value })
                }
                required
              />
            </label>
            <label>
              Lösenord
              <input
                type="password"
                name="registerPassword"
                placeholder="Minst 8 tecken"
                value={registerForm.password}
                onChange={(event) =>
                  setRegisterForm({ ...registerForm, password: event.target.value })
                }
                required
              />
            </label>
            <label>
              Bekräfta lösenord
              <input
                type="password"
                name="registerConfirmPassword"
                placeholder="Upprepa lösenord"
                value={registerForm.confirmPassword}
                onChange={(event) =>
                  setRegisterForm({ ...registerForm, confirmPassword: event.target.value })
                }
                required
              />
            </label>
            {showPasswordFeedback && (
              <ul className="password-feedback">
                <li className={meetsPasswordPolicy ? 'valid' : 'invalid'}>
                  {meetsPasswordPolicy ? '✓' : '✗'} Uppfyller lösenordskraven (minst 8 tecken, stor bokstav och specialtecken)
                </li>
                <li className={passwordsMatch ? 'valid' : 'invalid'}>
                  {passwordsMatch ? '✓' : '✗'} Lösenorden matchar
                </li>
              </ul>
            )}
            {renderMessage('register')}
            <button
              className="pill-button"
              type="submit"
              disabled={
                loading === 'register' || !meetsPasswordPolicy || !passwordsMatch
              }
            >
              {loading === 'register' ? 'Skapar…' : 'Registrera'}
            </button>
          </form>
        </>
      )
      break
    case 'profile':
      panelContent = (
        <>
          <h2>Min profil</h2>
          {token ? (
            <>
              <div className="panel-actions">
                <button
                  className="pill-button"
                  type="button"
                  onClick={handleRefreshProfile}
                  disabled={loading === 'profile'}
                >
                  {loading === 'profile' ? 'Hämtar…' : 'Uppdatera profil'}
                </button>
              </div>
              {renderMessage('profile')}
              {profile ? (
                <div className="auth-details">
                  <dl>
                    <dt>Namn</dt>
                    <dd>
                      {profile.firstName} {profile.lastName}
                    </dd>
                    <dt>E-post</dt>
                    <dd>{profile.email}</dd>
                    <dt>Roll</dt>
                    <dd>{profile.role}</dd>
                    <dt>ID</dt>
                    <dd>{profile.id}</dd>
                  </dl>
                </div>
              ) : (
                loading === 'profile' ? null : (
                  <p className="auth-note">Ingen profildata hämtad ännu.</p>
                )
              )}
              <section className="subscription-card">
                <div
                  className={`subscription-chip ${
                    subscriptionState.phase === 'success'
                      ? subscriptionState.hasAccess
                        ? 'active'
                        : 'inactive'
                      : subscriptionState.phase === 'error'
                        ? 'error'
                        : 'pending'
                  }`}
                >
                  Prenumerationsstatus
                </div>

                {subscriptionState.phase === 'loading' && (
                  <p className="auth-note">Kontrollerar prenumeration…</p>
                )}

                {subscriptionState.phase === 'error' && (
                  <p className="subscription-error">{subscriptionState.error}</p>
                )}

                {subscriptionState.phase === 'success' && (
                  subscriptionState.hasAccess ? (
                    <div className="subscription-meta">
                      <div>
                        <span>Plan</span>
                        <strong>{subscriptionState.detail?.plan ?? 'Okänd'}</strong>
                      </div>
                      <div>
                        <span>Status</span>
                        <strong>
                          {subscriptionState.detail
                            ? translateStatus(subscriptionState.detail.status)
                            : 'Aktiv'}
                        </strong>
                      </div>
                      {subscriptionState.detail && (
                        <>
                          <div>
                            <span>Startade</span>
                            <strong>{formatDateTime(subscriptionState.detail.startAt)}</strong>
                          </div>
                          <div>
                            <span>Giltig till</span>
                            <strong>{formatDateTime(subscriptionState.detail.endAt)}</strong>
                          </div>
                        </>
                      )}
                    </div>
                  ) : (
                    <p className="auth-note">Ingen aktiv prenumeration.</p>
                  )
                )}

                {subscriptionState.phase === 'idle' && (
                  <p className="auth-note">Prenumerationsstatus hämtas efter inloggning.</p>
                )}
              </section>
            </>
          ) : (
            <p className="auth-note">Logga in för att se din profil.</p>
          )}
        </>
      )
      break
    case 'settings':
      panelContent = (
        <>
          <h2>Inställningar</h2>
          {renderMessage('settings')}
          <form className="auth-form" onSubmit={handleSettingsSubmit}>
            <label>
              Språk (locale)
              <input
                type="text"
                name="settingsLocale"
                placeholder="sv-SE"
                value={settingsForm.locale}
                onChange={handleSettingsInput('locale')}
                disabled={!token || loading === 'settings'}
                required
              />
            </label>
            <label>
              Tidszon
              <input
                type="text"
                name="settingsTimezone"
                placeholder="Europe/Stockholm"
                value={settingsForm.timezone}
                onChange={handleSettingsInput('timezone')}
                disabled={!token || loading === 'settings'}
                required
              />
            </label>
            <label className="checkbox-row">
              <input
                type="checkbox"
                name="settingsMarketing"
                checked={settingsForm.marketingOptIn}
                onChange={handleSettingsCheckbox('marketingOptIn')}
                disabled={!token || loading === 'settings'}
              />
              <span>Ta emot nyheter och uppdateringar</span>
            </label>
            <button className="pill-button" type="submit" disabled={!token || loading === 'settings'}>
              {loading === 'settings' ? 'Sparar…' : 'Spara inställningar'}
            </button>
          </form>
          {!token && <p className="auth-note">Logga in för att kunna spara.</p>}
        </>
      )
      break
    case 'report':
      panelContent = (
        <>
          <h2>Senaste rapport</h2>
          <p className="auth-note">
            Visar resultatet från reporter-service. Kräver aktiv prenumeration.
          </p>
          {reportState.phase === 'loading' && <p className="auth-note">Hämtar rapport…</p>}
          {reportState.phase === 'error' && (
            <p className="subscription-error">{reportState.error ?? 'Ett fel inträffade.'}</p>
          )}
          {reportState.phase === 'success' && reportState.report && (
            <article className="report-preview">
              <header className="report-preview__header">
                <div>
                  <span className="chip">Senaste rapport</span>
                  <h3>{new Date(reportState.report.reportDate).toLocaleDateString('sv-SE')}</h3>
                </div>
                <div className="report-meta">
                  <span>Skapad</span>
                  <strong>{formatReportTimestamp(reportState.report)}</strong>
                </div>
              </header>
              <section className="report-summary">
                {renderReportSummary(reportState.report.summary)}
              </section>
            </article>
          )}
          {!token && <p className="auth-note">Logga in för att kunna läsa rapporten.</p>}
        </>
      )
      break
    case 'subscribe':
      panelContent = (
        <>
          <h2>Prenumerera</h2>
          <p className="auth-note">Välj en plan och betala med Monero (XMR)</p>
          {renderMessage('subscribe')}
          {paymentState.phase === 'idle' || paymentState.phase === 'selecting' ? (
            <div className="subscription-plans">
              <div className="plan-card">
                <h3>Månad</h3>
                <div className="plan-price">
                  <span className="price-amount">0.05</span>
                  <span className="price-currency">XMR</span>
                </div>
                <p className="plan-description">Tillgång i 30 dagar</p>
                <button
                  className="pill-button"
                  type="button"
                  onClick={() => void handleSelectPlan('monthly')}
                  disabled={paymentState.phase === 'selecting'}
                >
                  Välj månad
                </button>
              </div>
              <div className="plan-card plan-card--featured">
                <span className="plan-badge">Bäst värde</span>
                <h3>År</h3>
                <div className="plan-price">
                  <span className="price-amount">0.50</span>
                  <span className="price-currency">XMR</span>
                </div>
                <p className="plan-description">Tillgång i 365 dagar</p>
                <p className="plan-savings">Spara 17% jämfört med månad</p>
                <button
                  className="pill-button"
                  type="button"
                  onClick={() => void handleSelectPlan('yearly')}
                  disabled={paymentState.phase === 'selecting'}
                >
                  Välj år
                </button>
              </div>
            </div>
          ) : null}
          {paymentState.phase === 'pending' || paymentState.phase === 'polling' ? (
            <div className="payment-details">
              <h3>Skicka betalning</h3>
              <p className="auth-note">
                Skicka exakt <strong>{paymentState.payment?.amountXmr} XMR</strong> till adressen nedan
              </p>
              <div className="payment-address">
                <label>Monero-adress</label>
                <code className="payment-address-code">{paymentState.payment?.paymentAddress}</code>
                <button
                  className="copy-button"
                  type="button"
                  onClick={() => {
                    if (paymentState.payment?.paymentAddress) {
                      void navigator.clipboard.writeText(paymentState.payment.paymentAddress)
                      setMessage({ scope: 'subscribe', status: 'success', text: 'Adress kopierad!' })
                    }
                  }}
                >
                  Kopiera
                </button>
              </div>
              <div className="payment-qr">
                <p className="auth-note">QR-kod kommer här (TODO)</p>
              </div>
              <div className="payment-test-info">
                <p className="auth-note">
                  <strong>För testning:</strong> Bekräfta betalningen manuellt med PowerShell:
                </p>
                <code className="payment-test-command">
                  .\confirm-payment.ps1 -PaymentId {paymentState.payment?.paymentId}
                </code>
                <button
                  className="copy-button copy-button--small"
                  type="button"
                  onClick={() => {
                    if (paymentState.payment?.paymentId) {
                      void navigator.clipboard.writeText(
                        `.\\confirm-payment.ps1 -PaymentId ${paymentState.payment.paymentId}`
                      )
                      setMessage({ scope: 'subscribe', status: 'success', text: 'Kommando kopierat!' })
                    }
                  }}
                >
                  Kopiera kommando
                </button>
              </div>
              {paymentState.payment?.expiresAt && (
                <p className="payment-expiry">
                  Betalningen går ut:{' '}
                  {new Date(paymentState.payment.expiresAt).toLocaleString('sv-SE')}
                </p>
              )}
              {paymentState.phase === 'polling' && (
                <p className="payment-status">⏳ Väntar på betalning...</p>
              )}
            </div>
          ) : null}
          {paymentState.phase === 'confirmed' ? (
            <div className="payment-success">
              <h3>✓ Betalning bekräftad!</h3>
              <p>Din prenumeration är nu aktiv.</p>
              <button className="pill-button" type="button" onClick={() => handleChangeView('report')}>
                Visa rapporter
              </button>
            </div>
          ) : null}
          {paymentState.phase === 'expired' ? (
            <div className="payment-error">
              <h3>Betalningen gick ut</h3>
              <p>Betalningen tog för lång tid. Försök igen.</p>
              <button
                className="pill-button"
                type="button"
                onClick={() => setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })}
              >
                Försök igen
              </button>
            </div>
          ) : null}
          {paymentState.phase === 'error' && paymentState.error ? (
            <div className="payment-error">
              <p>{paymentState.error}</p>
              <button
                className="pill-button"
                type="button"
                onClick={() => setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })}
              >
                Försök igen
              </button>
            </div>
          ) : null}
        </>
      )
      break
  }

  return (
    <div className="auth-shell">
      <aside className="auth-sidebar">
        <div>
          <h1>NovaReport</h1>
          <p>Simple access för accounts-service</p>
        </div>
        <nav className="auth-nav">
          <button
            type="button"
            className={`${view === 'login' ? 'active' : ''}`}
            onClick={() => handleChangeView('login')}
          >
            Logga in
          </button>
          <button
            type="button"
            className={`${view === 'register' ? 'active' : ''}`}
            onClick={() => handleChangeView('register')}
          >
            Skapa konto
          </button>
          <button
            type="button"
            className={`${view === 'profile' ? 'active' : ''} ${token ? '' : 'locked'}`.trim()}
            onClick={() => handleChangeView('profile')}
          >
            Min profil
          </button>
          <button
            type="button"
            className={`${view === 'settings' ? 'active' : ''} ${token ? '' : 'locked'}`.trim()}
            onClick={() => handleChangeView('settings')}
          >
            Inställningar
          </button>
          <button
            type="button"
            className={`${view === 'subscribe' ? 'active' : ''} ${token ? '' : 'locked'}`.trim()}
            onClick={() => handleChangeView('subscribe')}
          >
            Prenumerera
          </button>
          <button
            type="button"
            className={`${view === 'report' ? 'active' : ''}`}
            onClick={() => handleChangeView('report')}
          >
            Rapport
          </button>
        </nav>
      </aside>

      <main className="auth-content">
        <section className="auth-panel">{panelContent}</section>
      </main>
    </div>
  )
}

export default App
