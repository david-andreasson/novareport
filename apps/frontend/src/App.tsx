import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent, JSX } from 'react'
import './App.css'

const API_BASE = 'http://localhost:8080'
const SUBS_API_BASE = (import.meta.env.VITE_SUBS_API_BASE as string | undefined) ?? API_BASE
const NOTIF_API_BASE = (import.meta.env.VITE_NOTIF_API_BASE as string | undefined) ?? 'http://localhost:8083'
const REPORTER_API_BASE =
  (import.meta.env.VITE_REPORTER_API_BASE as string | undefined) ?? 'http://localhost:8082'
const INTERNAL_API_KEY = (import.meta.env.VITE_INTERNAL_API_KEY as string | undefined) ?? ''

const FOUR_HOURS_MS = 4 * 60 * 60 * 1000

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

type View = 'login' | 'register' | 'profile' | 'settings' | 'report'

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
    twoFactorEnabled: false,
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

  const isLoadingReport = reportState.phase === 'loading'

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
      const accessResponse = await fetch(`${SUBS_API_BASE}/api/v1/subscriptions/me/has-access`, {
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
        const detailResponse = await fetch(`${SUBS_API_BASE}/api/v1/subscriptions/me`, {
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
    const response = await fetch(`${NOTIF_API_BASE}/api/v1/notifications/latest`, {
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

  const isReportFresh = (report: DailyReport | null) => {
    if (!report?.createdAt) {
      return false
    }
    const createdAt = new Date(report.createdAt)
    if (Number.isNaN(createdAt.getTime())) {
      return false
    }
    return Date.now() - createdAt.getTime() <= FOUR_HOURS_MS
  }

  const triggerIngest = async () => {
    const response = await fetch(`${REPORTER_API_BASE}/api/v1/internal/reporter/ingest-now`, {
      method: 'POST',
      headers: {
        'X-INTERNAL-KEY': INTERNAL_API_KEY,
      },
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(errorText || 'Kunde inte hämta nyhetsdata inför rapportbygget.')
    }
  }

  const triggerReportBuild = async (date: string) => {
    const response = await fetch(
      `${REPORTER_API_BASE}/api/v1/internal/reporter/build-report?date=${date}`,
      {
        method: 'POST',
        headers: {
          'X-INTERNAL-KEY': INTERNAL_API_KEY,
        },
      },
    )

    if (!response.ok && response.status !== 202) {
      const errorText = await response.text()
      throw new Error(errorText || 'Kunde inte trigga rapportbygge.')
    }
  }

  const pollForNewReport = async (previousReportId?: string) => {
    const attempts = 6
    for (let i = 0; i < attempts; i += 1) {
      await delay(i === 0 ? 1000 : 1500)
      try {
        const candidate = await getLatestReport()
        if (!candidate) {
          continue
        }
        if (!previousReportId || candidate.reportId !== previousReportId || isReportFresh(candidate)) {
          return candidate
        }
      } catch (error) {
        if (error instanceof Error && error.message.includes('Logga in igen')) {
          throw error
        }
      }
    }
    return null
  }

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading('login')
    setMessage((prev) => (prev?.scope === 'login' ? null : prev))
    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
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
      const response = await fetch(`${API_BASE}/auth/register`, {
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
      const response = await fetch(`${API_BASE}/me`, {
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

  const handleSettingsCheckbox = (field: 'marketingOptIn' | 'twoFactorEnabled') =>
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
      const response = await fetch(`${API_BASE}/me/settings`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(settingsForm),
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

  const handleEnsureFreshReport = async () => {
    if (!token) {
      setReportState({ phase: 'error', report: null, error: 'Logga in för att se rapporten.' })
      return
    }

    setReportState({ phase: 'loading', report: null })
    try {
      const current = await getLatestReport()
      if (current && isReportFresh(current)) {
        setReportState({ phase: 'success', report: current })
        return
      }

      if (!INTERNAL_API_KEY) {
        throw new Error('Intern API-nyckel saknas. Sätt VITE_INTERNAL_API_KEY i frontendens miljövariabler.')
      }

      await triggerIngest()

      const today = new Date().toISOString().slice(0, 10)
      await triggerReportBuild(today)

      const refreshed = await pollForNewReport(current?.reportId)
      if (refreshed) {
        setReportState({ phase: 'success', report: refreshed })
        return
      }

      throw new Error('Rapporten kunde inte hämtas efter byggförsök. Försök igen om en stund.')
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
            <label className="checkbox-row">
              <input
                type="checkbox"
                name="settingsTwoFactor"
                checked={settingsForm.twoFactorEnabled}
                onChange={handleSettingsCheckbox('twoFactorEnabled')}
                disabled={!token || loading === 'settings'}
              />
              <span>Aktivera tvåfaktorsautentisering</span>
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
          <h2>Senaste rapport (temporär vy)</h2>
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
                {reportState.report.createdAt && (
                  <div className="report-meta">
                    <span>Skapad</span>
                    <strong>{new Date(reportState.report.createdAt).toLocaleString('sv-SE')}</strong>
                  </div>
                )}
              </header>
              <section className="report-summary">
                {renderReportSummary(reportState.report.summary)}
              </section>
              <div className="report-actions">
                {!isReportFresh(reportState.report) && (
                  <p className="auth-note">Rapporten är äldre än 4 timmar. Klicka för att skapa en ny.</p>
                )}
                <button
                  className="pill-button"
                  type="button"
                  onClick={() => void handleEnsureFreshReport()}
                  disabled={isLoadingReport}
                >
                  {isReportFresh(reportState.report) ? 'Visa senaste igen' : 'Skapa ny rapport'}
                </button>
              </div>
            </article>
          )}
          {token && reportState.phase !== 'loading' && reportState.phase !== 'success' && (
            <button className="pill-button" type="button" onClick={() => void handleEnsureFreshReport()}>
              Skapa eller uppdatera rapport
            </button>
          )}
          {!token && <p className="auth-note">Logga in för att kunna läsa rapporten.</p>}
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
            className={`${view === 'report' ? 'active' : ''}`}
            onClick={() => handleChangeView('report')}
          >
            Rapport (test)
          </button>
        </nav>
        {token && (
          <div className="auth-token">
            <span>Bearer-token:</span>
            <code>{token}</code>
          </div>
        )}
      </aside>

      <main className="auth-content">
        <section className="auth-panel">{panelContent}</section>
      </main>
    </div>
  )
}

export default App
