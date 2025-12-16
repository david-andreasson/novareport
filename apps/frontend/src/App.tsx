import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent, JSX } from 'react'
import './AppDark.css'
import { LoginPanel } from './components/LoginPanel'
import { RegisterPanel } from './components/RegisterPanel'
import { ProfilePanel } from './components/ProfilePanel'
import { ReportPanel } from './components/ReportPanel'
import { SubscribePanel } from './components/SubscribePanel'
import { StripeSubscribePanel, type StripePaymentState } from './components/StripeSubscribePanel'
import { AdminPanel } from './components/AdminPanel'
import { login, register, getProfile, updateSettings } from './api/accounts'
import { getSubscriptionInfo } from './api/subscriptions'
import { getLatestReport as apiGetLatestReport, requestDiscordInvite } from './api/reports'
import { createPayment, getPaymentStatus } from './api/payments'
import { createStripePaymentIntent } from './api/paymentsStripe'

export const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

type View = 'login' | 'register' | 'profile' | 'settings' | 'report' | 'subscribe' | 'admin'

type Message = {
  scope: View
  status: 'success' | 'error'
  text: string
}

export type UserProfile = {
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

export type SubscriptionState = {
  phase: 'idle' | 'loading' | 'success' | 'error'
  hasAccess: boolean | null
  detail: SubscriptionDetail | null
  error?: string
}

export type DailyReport = {
  id?: string
  reportId?: string
  reportDate: string
  summary: string
  createdAt?: string
  updatedAt?: string
}

export type LatestReportState = {
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

export type PaymentState = {
  phase: 'idle' | 'selecting' | 'pending' | 'polling' | 'confirmed' | 'expired' | 'error'
  selectedPlan: 'monthly' | 'yearly' | null
  payment: PaymentInfo | null
  error?: string
}

export const translateStatus = (status: string) => {
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

export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('sv-SE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })

export const formatReportTimestamp = (report: DailyReport) => {
  const iso = report.updatedAt ?? report.createdAt ?? report.reportDate
  return formatDateTime(iso)
}

export const renderInlineMarkdown = (text: string): (string | JSX.Element)[] => {
  const parts = text.split(/(\*\*[^*]+\*\*)/)
  return parts.map((part, index) => {
    const boldMatch = part.match(/^\*\*([^*]+)\*\*$/)
    if (boldMatch) {
      return (
        <strong key={index}>
          {boldMatch[1]}
        </strong>
      )
    }
    return part
  })
}

export const renderReportSummary = (summary: string) => {
  const rawLines = summary.split('\n')

  const elements: JSX.Element[] = []
  let paragraphLines: string[] = []
  let listItems: string[] | null = null

  const flushParagraph = () => {
    if (paragraphLines.length > 0) {
      const text = paragraphLines.join(' ')
      elements.push(
        <p key={`p-${elements.length}`} className="report-summary__paragraph">
          {renderInlineMarkdown(text)}
        </p>,
      )
      paragraphLines = []
    }
  }

  const flushList = () => {
    if (listItems && listItems.length > 0) {
      elements.push(
        <ul key={`ul-${elements.length}`} className="report-summary__list">
          {listItems.map((item, index) => (
            <li key={index}>{renderInlineMarkdown(item)}</li>
          ))}
        </ul>,
      )
      listItems = null
    }
  }

  for (let i = 0; i < rawLines.length; i++) {
    const line = rawLines[i].trim()

    if (line.length === 0) {
      flushParagraph()
      flushList()
      continue
    }

    const headingMatch = line.match(/^(#{1,6})\s+(.*)$/)
    if (headingMatch) {
      flushParagraph()
      flushList()

      const level = headingMatch[1].length
      const rawText = headingMatch[2].trim()
      // Remove leading numeric prefixes like "1. ", "2) " to avoid cluttered headings
      const cleanedText = rawText.replace(/^[0-9]+[.)]\s*/, '') || rawText

      let heading: JSX.Element
      switch (level) {
        case 1:
          heading = (
            <h1 key={`h-${elements.length}`} className="report-summary__heading report-summary__heading--h1">
              {cleanedText}
            </h1>
          )
          break
        case 2:
          heading = (
            <h2 key={`h-${elements.length}`} className="report-summary__heading report-summary__heading--h2">
              {cleanedText}
            </h2>
          )
          break
        case 3:
          heading = (
            <h3 key={`h-${elements.length}`} className="report-summary__heading report-summary__heading--h3">
              {cleanedText}
            </h3>
          )
          break
        default:
          heading = (
            <h4 key={`h-${elements.length}`} className="report-summary__heading report-summary__heading--h4">
              {cleanedText}
            </h4>
          )
          break
      }

      elements.push(heading)
      continue
    }

    // Lines that are only bold markdown ("**Heading**") – treat as headings too
    const boldLineMatch = line.match(/^\*\*(.+)\*\*$/)
    if (boldLineMatch) {
      flushParagraph()
      flushList()

      const rawText = boldLineMatch[1].trim()
      const cleanedText = rawText.replace(/^[0-9]+[.)]\s*/, '') || rawText

      const heading = (
        <h2
          key={`h-${elements.length}`}
          className="report-summary__heading report-summary__heading--h2"
        >
          {cleanedText}
        </h2>
      )

      elements.push(heading)
      continue
    }

    if (line.startsWith('- ')) {
      flushParagraph()
      if (!listItems) {
        listItems = []
      }
      listItems.push(line.replace(/^-\s*/, ''))
      continue
    }

    if (listItems) {
      flushList()
    }

    paragraphLines.push(line)
  }

  flushParagraph()
  flushList()

  if (elements.length === 0) {
    return <p className="report-summary__empty">Ingen sammanfattning tillgänglig.</p>
  }

  return <>{elements}</>
}

function App() {
  const getInitialView = (): View => {
    try {
      const params = new URLSearchParams(window.location.search)
      const viewParam = params.get('view')
      const allowed: View[] = ['login', 'register', 'profile', 'settings', 'report', 'subscribe', 'admin']
      if (viewParam && (allowed as string[]).includes(viewParam)) {
        return viewParam as View
      }
    } catch {
      // Faller tillbaka på standardvy om window eller URLSearchParams inte finns
    }
    return 'login'
  }

  const [view, setView] = useState<View>(() => getInitialView())
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
    reportEmailOptIn: false,
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
  const [paymentMethod, setPaymentMethod] = useState<'monero' | 'stripe'>('monero')
  const [stripePaymentState, setStripePaymentState] = useState<StripePaymentState>({
    phase: 'idle',
    selectedPlan: null,
    paymentId: null,
    clientSecret: null,
    amountFiat: null,
    currencyFiat: null,
  })

  const passwordPolicyRegex = /^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/
  const meetsPasswordPolicy = passwordPolicyRegex.test(registerForm.password)
  const passwordsMatch =
    registerForm.password.length > 0 &&
    registerForm.confirmPassword.length > 0 &&
    registerForm.password === registerForm.confirmPassword
  const showPasswordFeedback =
    registerForm.password.length > 0 || registerForm.confirmPassword.length > 0

  const getViewTitle = (current: View): string => {
    switch (current) {
      case 'login':
        return 'Logga in'
      case 'register':
        return 'Skapa konto'
      case 'profile':
      case 'settings':
        return 'Konto & inställningar'
      case 'report':
        return 'Rapport'
      case 'subscribe':
        return 'Prenumeration'
      case 'admin':
        return 'Admin'
      default:
        return 'NovaReport'
    }
  }

  const profileInitials =
    profile != null && profile.firstName.length > 0 && profile.lastName.length > 0
      ? `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`.toUpperCase()
      : 'NR'

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
      const { hasAccess, detail } = await getSubscriptionInfo(token)
      setSubscriptionState({ phase: 'success', hasAccess, detail })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setSubscriptionState({ phase: 'error', hasAccess: null, detail: null, error: text })
    }
  }


  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading('login')
    setMessage((prev) => (prev?.scope === 'login' ? null : prev))
    try {
      const data = await login(loginForm.email, loginForm.password)
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
      const data = await register({
        email: registerForm.email,
        password: registerForm.password,
        firstName: registerForm.firstName,
        lastName: registerForm.lastName,
      })
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
      const data: UserProfile = await getProfile(token)
      setProfile(data)
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setProfile(null)
      setMessage({ scope: 'profile', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const handleSettingsCheckbox = (field: 'marketingOptIn' | 'reportEmailOptIn') =>
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
      await updateSettings(token, { ...settingsForm, twoFactorEnabled: false })
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
      const data = await apiGetLatestReport(token)
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

  const handleRequestDiscordInvite = async () => {
    if (!token) {
      setMessage({
        scope: 'profile',
        status: 'error',
        text: 'Logga in för att begära Discord-invite.',
      })
      return
    }

    if (subscriptionState.phase !== 'success' || !subscriptionState.hasAccess) {
      setMessage({
        scope: 'profile',
        status: 'error',
        text: 'Du behöver en aktiv prenumeration för att få Discord-invite.',
      })
      return
    }

    setLoading('profile')
    setMessage((prev) => (prev?.scope === 'profile' ? null : prev))
    try {
      await requestDiscordInvite(token)
      setMessage({
        scope: 'profile',
        status: 'success',
        text: 'Discord-invite har skickats till din e-postadress.',
      })
    } catch (error) {
      const text =
        error instanceof Error ? error.message : 'Kunde inte skicka Discord-invite.'
      setMessage({ scope: 'profile', status: 'error', text })
    } finally {
      setLoading(null)
    }
  }

  const handleSelectPlan = async (plan: 'monthly' | 'yearly') => {
    if (!token) {
      setMessage({ scope: 'subscribe', status: 'error', text: 'Logga in för att prenumerera.' })
      return
    }

    setPaymentState({ phase: 'selecting', selectedPlan: plan, payment: null })
    setMessage(null)

    try {
      const amountXmr = plan === 'monthly' ? '0.0001' : '0.005'
      const data = await createPayment(token, plan, amountXmr)
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

  const resetStripePaymentState = () => {
    setStripePaymentState({
      phase: 'idle',
      selectedPlan: null,
      paymentId: null,
      clientSecret: null,
      amountFiat: null,
      currencyFiat: null,
    })
  }

  const handleSelectStripePlan = async (plan: 'monthly' | 'yearly') => {
    if (!token) {
      setMessage({ scope: 'subscribe', status: 'error', text: 'Logga in för att prenumerera.' })
      return
    }

    setStripePaymentState({
      phase: 'processing',
      selectedPlan: plan,
      paymentId: null,
      clientSecret: null,
      amountFiat: null,
      currencyFiat: null,
    })
    setMessage(null)

    try {
      const data = await createStripePaymentIntent(token, plan)
      setStripePaymentState({
        phase: 'intentCreated',
        selectedPlan: plan,
        paymentId: data.paymentId,
        clientSecret: data.clientSecret,
        amountFiat: data.amountFiat,
        currencyFiat: data.currencyFiat,
      })
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Okänt fel'
      setStripePaymentState({
        phase: 'error',
        selectedPlan: null,
        paymentId: null,
        clientSecret: null,
        amountFiat: null,
        currencyFiat: null,
        error: text,
      })
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

        const data = await getPaymentStatus(token, paymentId)

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
    if (view === 'profile' || view === 'settings') {
      if (!token) {
        setProfile(null)
        setSubscriptionState({ phase: 'idle', hasAccess: null, detail: null })
        setMessage({
          scope: view === 'settings' ? 'settings' : 'profile',
          status: 'error',
          text:
            view === 'settings'
              ? 'Logga in för att ändra inställningar.'
              : 'Logga in för att se profilen.',
        })
        return
      }
      void fetchProfile()
      void fetchSubscriptionInfo()
    } else if (view === 'report') {
      void fetchLatestReport()
    } else if (view === 'subscribe') {
      // Reset payment state when entering subscribe view
      setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
      resetStripePaymentState()
      if (!token) {
        setMessage({ scope: 'subscribe', status: 'error', text: 'Logga in för att prenumerera.' })
      }
    }
  }, [view, token])

  let panelContent: JSX.Element

  switch (view) {
    case 'login':
      panelContent = (
        <LoginPanel
          loginForm={loginForm}
          onChange={(form) => setLoginForm(form)}
          onSubmit={handleLoginSubmit}
          message={renderMessage('login')}
          isLoading={loading === 'login'}
        />
      )
      break
    case 'register':
      panelContent = (
        <RegisterPanel
          registerForm={registerForm}
          onChange={(form) => setRegisterForm(form)}
          onSubmit={handleRegisterSubmit}
          showPasswordFeedback={showPasswordFeedback}
          meetsPasswordPolicy={meetsPasswordPolicy}
          passwordsMatch={passwordsMatch}
          message={renderMessage('register')}
          isLoading={loading === 'register'}
        />
      )
      break
    case 'profile':
    case 'settings':
      panelContent = (
        <ProfilePanel
          token={token}
          isLoadingProfile={loading === 'profile'}
          isSavingSettings={loading === 'settings'}
          profileMessage={renderMessage('profile')}
          settingsMessage={renderMessage('settings')}
          profile={profile}
          subscriptionState={subscriptionState}
          settingsForm={settingsForm}
          onRefresh={handleRefreshProfile}
          formatDateTime={formatDateTime}
          translateStatus={translateStatus}
          onRequestDiscordInvite={handleRequestDiscordInvite}
          onSettingsSubmit={handleSettingsSubmit}
          onToggleMarketing={handleSettingsCheckbox('marketingOptIn')}
          onToggleReportEmail={handleSettingsCheckbox('reportEmailOptIn')}
        />
      )
      break
    case 'report':
      panelContent = (
        <ReportPanel
          token={token}
          reportState={reportState}
          formatTimestamp={formatReportTimestamp}
          renderSummary={renderReportSummary}
        />
      )
      break
    case 'subscribe': {
      panelContent = (
        <>
          <div className="payment-method-toggle">
            <button
              type="button"
              className={`pill-button ${paymentMethod === 'monero' ? 'pill-button--active' : ''}`.trim()}
              onClick={() => {
                setPaymentMethod('monero')
                setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
                resetStripePaymentState()
              }}
            >
              Betala med Monero (20% rabatt)
            </button>
            <button
              type="button"
              className={`pill-button ${paymentMethod === 'stripe' ? 'pill-button--active' : ''}`.trim()}
              onClick={() => {
                setPaymentMethod('stripe')
                setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
                resetStripePaymentState()
              }}
            >
              Kortbetalning
            </button>
          </div>

          {paymentMethod === 'monero' ? (
            <SubscribePanel
              paymentState={paymentState}
              message={renderMessage('subscribe')}
              onSelectPlan={(plan) => {
                void handleSelectPlan(plan)
              }}
              onNavigateToReport={() => handleChangeView('report')}
              onResetPayment={() =>
                setPaymentState({ phase: 'idle', selectedPlan: null, payment: null })
              }
              onCopyPaymentAddress={(address) => {
                void navigator.clipboard.writeText(address)
                setMessage({ scope: 'subscribe', status: 'success', text: 'Adress kopierad!' })
              }}
            />
          ) : (
            <StripeSubscribePanel
              state={stripePaymentState}
              message={renderMessage('subscribe')}
              onSelectPlan={(plan) => {
                void handleSelectStripePlan(plan)
              }}
              onPaymentSuccess={() => {
                setStripePaymentState((prev) => ({ ...prev, phase: 'confirmed' }))
                setMessage({
                  scope: 'subscribe',
                  status: 'success',
                  text: 'Kortbetalning genomförd! Din prenumeration aktiveras strax.',
                })
                void fetchSubscriptionInfo()
              }}
              onPaymentError={(text) => {
                setStripePaymentState((prev) => ({ ...prev, phase: 'error', error: text }))
                setMessage({ scope: 'subscribe', status: 'error', text })
              }}
            />
          )}
        </>
      )
      break
    }
    case 'admin':
      panelContent = <AdminPanel token={token} profile={profile} />
      break
  }

  return (
    <div className="auth-shell">
      <aside className="auth-sidebar">
        <div>
          <h1>NovaReport</h1>
          <p>Daily Crypto Reports</p>
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
            className={`${
              view === 'profile' || view === 'settings' ? 'active' : ''
            } ${token ? '' : 'locked'}`.trim()}
            onClick={() => handleChangeView('profile')}
          >
            Konto & inställningar
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
          {profile?.role === 'ADMIN' && (
            <button
              type="button"
              className={`${view === 'admin' ? 'active' : ''}`}
              onClick={() => handleChangeView('admin')}
            >
              Admin
            </button>
          )}
        </nav>
      </aside>

      <main className="auth-content">
        <header className="auth-topbar">
          <div className="auth-topbar__title">
            <h2>{getViewTitle(view)}</h2>
            {view !== 'report' && (
              <p className="auth-topbar__subtitle">
                {view === 'profile' || view === 'settings'
                  ? 'Översikt över ditt konto, prenumeration och e-postinställningar.'
                  : view === 'subscribe'
                    ? 'Starta eller förläng din NovaReport-prenumeration.'
                    : view === 'admin'
                      ? 'Adminpanel för support och drift.'
                      : 'Logga in eller skapa konto för att komma igång.'}
              </p>
            )}
          </div>
          <div className="auth-topbar__user">
            <span className="auth-topbar__status">{token == null ? 'Inte inloggad' : 'Inloggad'}</span>
            <div className="auth-user-chip">
              <span className="auth-user-chip__initials">{profileInitials}</span>
              <span className="auth-user-chip__text">{profile?.email ?? 'Gäst'}</span>
            </div>
          </div>
        </header>

        <section
          className={`auth-panel ${view === 'report' ? 'auth-panel--report' : ''}`.trim()}
        >
          {panelContent}
        </section>
      </main>
    </div>
  )
}

export default App
