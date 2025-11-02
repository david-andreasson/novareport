import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent, JSX } from 'react'
import './App.css'

const API_BASE = 'http://localhost:8080'

type View = 'login' | 'register' | 'profile' | 'settings'

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

function App() {
  const [view, setView] = useState<View>('login')
  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const [registerForm, setRegisterForm] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  })
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

  const renderMessage = (scope: View) =>
    message?.scope === scope ? (
      <p className={`auth-feedback ${message.status}`}>{message.text}</p>
    ) : null

  const handleChangeView = (nextView: View) => {
    setView(nextView)
    setMessage((prev) => (prev?.scope === nextView ? prev : null))
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
      setMessage({ scope: 'profile', status: 'success', text: 'Konto skapat och inloggad' })
      setView('profile')
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

  useEffect(() => {
    if (view === 'profile') {
      if (!token) {
        setProfile(null)
        setMessage({ scope: 'profile', status: 'error', text: 'Logga in för att se profilen.' })
        return
      }
      void fetchProfile()
    } else if (view === 'settings' && !token) {
      setMessage({ scope: 'settings', status: 'error', text: 'Logga in för att ändra inställningar.' })
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
            {renderMessage('register')}
            <button className="pill-button" type="submit" disabled={loading === 'register'}>
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
                  onClick={fetchProfile}
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
