import { useEffect, useState, type FormEvent } from 'react'
import type { UserProfile } from '../App'
import { formatDateTime } from '../App'
import {
  getAccountsAdminMetrics,
  getNotificationsAdminMetrics,
  getSubscriptionsAdminMetrics,
  getPaymentsAdminMetrics,
  findUserByEmail,
  anonymizeUser,
  updateUserSettingsAdmin,
  resendWelcomeEmail,
  sendTestDailyReportEmail,
  getUserSubscriptionAdmin,
  getUserLastPaymentAdmin,
  runDailyReportNow,
  getDiscordAdminInfo,
  type AccountsAdminMetrics,
  type AdminUserDetails,
  type AdminUserSettings,
  type NotificationsAdminMetrics,
  type SubscriptionsAdminMetrics,
  type PaymentsAdminMetrics,
  type AdminSubscriptionDetail,
  type AdminPayment,
  type DiscordAdminInfo,
} from '../api/admin'

export type AdminPanelProps = {
  token: string | null
  profile: UserProfile | null
}

type AdminTab = 'overview' | 'user' | 'emails'

export function AdminPanel({ token, profile }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>('overview')
  const [accountsMetrics, setAccountsMetrics] = useState<AccountsAdminMetrics | null>(null)
  const [notificationsMetrics, setNotificationsMetrics] = useState<NotificationsAdminMetrics | null>(null)
  const [subscriptionsMetrics, setSubscriptionsMetrics] =
    useState<SubscriptionsAdminMetrics | null>(null)
  const [paymentsMetrics, setPaymentsMetrics] = useState<PaymentsAdminMetrics | null>(null)
  const [discordInfo, setDiscordInfo] = useState<DiscordAdminInfo | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [searchEmail, setSearchEmail] = useState('')
  const [selectedUser, setSelectedUser] = useState<AdminUserDetails | null>(null)
  const [userSettingsDraft, setUserSettingsDraft] = useState<AdminUserSettings | null>(null)
  const [userSubscription, setUserSubscription] = useState<AdminSubscriptionDetail | null>(null)
  const [userLastPayment, setUserLastPayment] = useState<AdminPayment | null>(null)

  useEffect(() => {
    if (!token) {
      setAccountsMetrics(null)
      setNotificationsMetrics(null)
      setSubscriptionsMetrics(null)
      setPaymentsMetrics(null)
      setDiscordInfo(null)
      return
    }

    setLoading(true)
    setError(null)

    Promise.all([
      getAccountsAdminMetrics(token),
      getNotificationsAdminMetrics(token),
      getSubscriptionsAdminMetrics(token),
      getPaymentsAdminMetrics(token),
      getDiscordAdminInfo(token),
    ])
      .then(([accounts, notifications, subscriptions, payments, discord]) => {
        setAccountsMetrics(accounts)
        setNotificationsMetrics(notifications)
        setSubscriptionsMetrics(subscriptions)
        setPaymentsMetrics(payments)
        setDiscordInfo(discord)
      })
      .catch((err) => {
        const message = err instanceof Error ? err.message : 'Kunde inte hämta admin-metrics.'
        setError(message)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [token])

  const handleSearchUser = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) {
      setError('Logga in som admin för att använda adminpanelen.')
      return
    }

    if (!searchEmail.trim()) {
      setError('Ange en e-postadress att söka på.')
      return
    }

    setLoading(true)
    setError(null)
    try {
      const user = await findUserByEmail(token, searchEmail.trim())
      setSelectedUser(user)
      setUserSettingsDraft(user.settings)

      const [subscription, lastPayment] = await Promise.all([
        getUserSubscriptionAdmin(token, user.id),
        getUserLastPaymentAdmin(token, user.id),
      ])

      setUserSubscription(subscription)
      setUserLastPayment(lastPayment)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte hämta användare.'
      setError(message)
      setSelectedUser(null)
      setUserSettingsDraft(null)
      setUserSubscription(null)
      setUserLastPayment(null)
    } finally {
      setLoading(false)
    }
  }

  const handleRunDailyReportNow = async () => {
    if (!token) {
      setError('Logga in som admin för att använda adminpanelen.')
      return
    }

    setLoading(true)
    setError(null)
    try {
      await runDailyReportNow(token)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte köra daglig rapport nu.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleAnonymizeUser = async () => {
    if (!token || !selectedUser) {
      return
    }

    if (!window.confirm('Är du säker på att du vill frigöra/anonymisera den här användarens e-postadress?')) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      const updated = await anonymizeUser(token, selectedUser.id)
      setSelectedUser(updated)
      setUserSettingsDraft(updated.settings)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte anonymisera användare.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleSaveUserSettings = async () => {
    if (!token || !selectedUser || !userSettingsDraft) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      const updated = await updateUserSettingsAdmin(token, selectedUser.id, userSettingsDraft)
      setSelectedUser(updated)
      setUserSettingsDraft(updated.settings)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte spara användarinställningar.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleResendWelcomeEmail = async () => {
    if (!token || !selectedUser) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      await resendWelcomeEmail(token, selectedUser.email, selectedUser.firstName)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte skicka välkomstmail igen.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleSendTestDailyReport = async () => {
    if (!token || !selectedUser) {
      return
    }

    setLoading(true)
    setError(null)
    try {
      await sendTestDailyReportEmail(token, selectedUser.email)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Kunde inte skicka testrapport.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const renderOverview = () => {
    if (!token) {
      return <p>Logga in som admin för att se admin-metrics.</p>
    }

    if (loading && !accountsMetrics && !notificationsMetrics) {
      return <p>Laddar admin-metrics…</p>
    }

    return (
      <div className="admin-overview">
        {accountsMetrics && (
          <div className="admin-metrics-group">
            <h3>Konton</h3>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Registrerade användare</div>
                <div className="admin-metric-value">{accountsMetrics.totalUsers}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Aktiva användare</div>
                <div className="admin-metric-value">{accountsMetrics.activeUsers}</div>
              </div>
            </div>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Inloggningar (OK)</div>
                <div className="admin-metric-value">{accountsMetrics.logins.success}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Felaktiga uppgifter</div>
                <div className="admin-metric-value">{accountsMetrics.logins.invalidCredentials}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Tekniska fel</div>
                <div className="admin-metric-value">{accountsMetrics.logins.error}</div>
              </div>
            </div>
          </div>
        )}

        {notificationsMetrics && (
          <div className="admin-metrics-group">
            <h3>E-post</h3>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Välkomstmail (OK)</div>
                <div className="admin-metric-value">{notificationsMetrics.welcomeEmails.success}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Välkomstmail (fel)</div>
                <div className="admin-metric-value">{notificationsMetrics.welcomeEmails.error}</div>
              </div>
            </div>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Dagliga mail (OK)</div>
                <div className="admin-metric-value">{notificationsMetrics.dailyEmails.success}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Mottagare totalt</div>
                <div className="admin-metric-value">{notificationsMetrics.dailyEmails.recipientsTotal}</div>
              </div>
            </div>
            {notificationsMetrics.latestReportDate && (
              <p className="admin-overview__note">
                Senaste rapportdatum: {notificationsMetrics.latestReportDate}{' '}
                {notificationsMetrics.latestReportEmailSent ? '(e-post skickad)' : '(e-post ej skickad)'}
              </p>
            )}
          </div>
        )}

        {subscriptionsMetrics && (
          <div className="admin-metrics-group">
            <h3>Prenumerationer</h3>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Aktiva prenumerationer</div>
                <div className="admin-metric-value">{subscriptionsMetrics.activeSubscriptions}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Aktiveringar (OK)</div>
                <div className="admin-metric-value">{subscriptionsMetrics.activatedSuccess}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Aktiveringar (fel)</div>
                <div className="admin-metric-value">{subscriptionsMetrics.activatedError}</div>
              </div>
            </div>
          </div>
        )}

        {paymentsMetrics && (
          <div className="admin-metrics-group">
            <h3>Betalningar</h3>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Skapade betalningar (OK)</div>
                <div className="admin-metric-value">{paymentsMetrics.created.success}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Skapade betalningar (fel)</div>
                <div className="admin-metric-value">{paymentsMetrics.created.error}</div>
              </div>
            </div>
            <div className="admin-metrics-row">
              <div className="admin-metric-card">
                <div className="admin-metric-label">Bekräftade betalningar (OK)</div>
                <div className="admin-metric-value">{paymentsMetrics.confirmed.success}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Bekräftade betalningar (ogiltigt tillstånd)</div>
                <div className="admin-metric-value">{paymentsMetrics.confirmed.invalidState}</div>
              </div>
              <div className="admin-metric-card">
                <div className="admin-metric-label">Bekräftade betalningar (fel)</div>
                <div className="admin-metric-value">{paymentsMetrics.confirmed.error}</div>
              </div>
            </div>
          </div>
        )}

        {discordInfo && (
          <div className="admin-metrics-group">
            <h3>Discord</h3>
            <p>
              Status:{' '}
              <strong>{discordInfo.configured ? 'Konfigurerad' : 'Inte konfigurerad'}</strong>
            </p>
            {discordInfo.inviteUrl && (
              <p>
                <a href={discordInfo.inviteUrl} target="_blank" rel="noreferrer">
                  Öppna Discord-invite
                </a>
              </p>
            )}
          </div>
        )}
      </div>
    )
  }

  const renderUserTab = () => (
    <div className="admin-user">
      <form className="admin-user__search" onSubmit={handleSearchUser}>
        <label>
          E-postadress
          <input
            type="email"
            value={searchEmail}
            onChange={(e) => setSearchEmail(e.target.value)}
            placeholder="user@example.com"
          />
        </label>
        <button type="submit" disabled={loading}>
          Sök
        </button>
      </form>

      {selectedUser && (
        <div className="admin-user__details">
          <div className="admin-user__summary">
            <h3>Användardetaljer</h3>
            <p>
              <strong>ID:</strong> {selectedUser.id}
            </p>
            <p>
              <strong>E-post:</strong> {selectedUser.email}
            </p>
            <p>
              <strong>Namn:</strong> {(selectedUser.firstName ?? '') + ' ' + (selectedUser.lastName ?? '')}
            </p>
            <p>
              <strong>Roll:</strong> {selectedUser.role}
            </p>
            <p>
              <strong>Status:</strong> {selectedUser.active ? 'Aktiv' : 'Inaktiv'}
            </p>
            <p>
              <strong>Skapad:</strong> {formatDateTime(selectedUser.createdAt)}
            </p>
          </div>

          {userSettingsDraft && (
            <div className="admin-user__settings">
              <h3>Inställningar</h3>
              <div className="admin-user__settings-grid">
                <label>
                  Språk (locale)
                  <input
                    type="text"
                    value={userSettingsDraft.locale}
                    onChange={(e) =>
                      setUserSettingsDraft({
                        ...userSettingsDraft,
                        locale: e.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Tidszon
                  <input
                    type="text"
                    value={userSettingsDraft.timezone}
                    onChange={(e) =>
                      setUserSettingsDraft({
                        ...userSettingsDraft,
                        timezone: e.target.value,
                      })
                    }
                  />
                </label>
                <label className="admin-user__checkbox">
                  <input
                    type="checkbox"
                    checked={userSettingsDraft.marketingOptIn}
                    onChange={(e) =>
                      setUserSettingsDraft({
                        ...userSettingsDraft,
                        marketingOptIn: e.target.checked,
                      })
                    }
                  />
                  Marknadsföringsmail
                </label>
                <label className="admin-user__checkbox">
                  <input
                    type="checkbox"
                    checked={userSettingsDraft.reportEmailOptIn}
                    onChange={(e) =>
                      setUserSettingsDraft({
                        ...userSettingsDraft,
                        reportEmailOptIn: e.target.checked,
                      })
                    }
                  />
                  Daglig rapport via e-post
                </label>
              </div>

              <div className="admin-user__actions">
                <button type="button" onClick={handleSaveUserSettings} disabled={loading}>
                  Spara inställningar
                </button>
                <button type="button" onClick={handleAnonymizeUser} disabled={loading}>
                  Frigör e-post för testning
                </button>
              </div>
            </div>
          )}

          <div className="admin-user__subscription">
            <h3>Prenumeration</h3>
            {userSubscription ? (
              <>
                <p>
                  <strong>Plan:</strong> {userSubscription.plan}
                </p>
                <p>
                  <strong>Status:</strong> {userSubscription.status}
                </p>
                <p>
                  <strong>Gäller från:</strong> {formatDateTime(userSubscription.startAt)}
                </p>
                <p>
                  <strong>Gäller till:</strong> {formatDateTime(userSubscription.endAt)}
                </p>
              </>
            ) : (
              <p>Ingen aktiv prenumeration hittades för den här användaren.</p>
            )}
          </div>

          <div className="admin-user__payments">
            <h3>Senaste betalning</h3>
            {userLastPayment ? (
              <>
                <p>
                  <strong>ID:</strong> {userLastPayment.id}
                </p>
                <p>
                  <strong>Plan:</strong> {userLastPayment.plan}
                </p>
                <p>
                  <strong>Status:</strong> {userLastPayment.status}
                </p>
                <p>
                  <strong>Belopp (XMR):</strong> {userLastPayment.amountXmr}
                </p>
                <p>
                  <strong>Skapad:</strong> {formatDateTime(userLastPayment.createdAt)}
                </p>
                <p>
                  <strong>Bekräftad:</strong>{' '}
                  {userLastPayment.confirmedAt ? formatDateTime(userLastPayment.confirmedAt) : '–'}
                </p>
              </>
            ) : (
              <p>Ingen betalning hittades för den här användaren.</p>
            )}
          </div>
        </div>
      )}
    </div>
  )

  const renderEmailsTab = () => (
    <div className="admin-emails">
      <p>
        Här kan du göra adminåtgärder kopplade till e-post, t.ex. skicka om välkomstmail eller skicka en
        testrapport till en specifik adress.
      </p>
      <div className="admin-emails__global">
        <h3>Daglig rapport till alla aktiva</h3>
        <button type="button" onClick={handleRunDailyReportNow} disabled={loading}>
          Kör daglig rapport nu
        </button>
      </div>
      {!selectedUser && <p>Sök först upp en användare under fliken "Användare".</p>}

      {selectedUser && (
        <div className="admin-emails__actions">
          <p>
            Aktiv användare: <strong>{selectedUser.email}</strong>
          </p>
          <button type="button" onClick={handleResendWelcomeEmail} disabled={loading}>
            Skicka välkomstmail igen
          </button>
          <button type="button" onClick={handleSendTestDailyReport} disabled={loading}>
            Skicka testrapport till den här användaren
          </button>
        </div>
      )}
    </div>
  )

  const isAdmin = profile?.role === 'ADMIN'

  if (!isAdmin) {
    return <p>Endast administratörer kan använda adminpanelen.</p>
  }

  return (
    <div className="admin-panel">
      {error && <p className="auth-feedback error">{error}</p>}
      <div className="admin-tabs">
        <button
          type="button"
          className={activeTab === 'overview' ? 'active' : ''}
          onClick={() => setActiveTab('overview')}
        >
          Översikt
        </button>
        <button
          type="button"
          className={activeTab === 'user' ? 'active' : ''}
          onClick={() => setActiveTab('user')}
        >
          Användare
        </button>
        <button
          type="button"
          className={activeTab === 'emails' ? 'active' : ''}
          onClick={() => setActiveTab('emails')}
        >
          E-post & rapporter
        </button>
      </div>

      <div className="admin-tab-content">
        {activeTab === 'overview' && renderOverview()}
        {activeTab === 'user' && renderUserTab()}
        {activeTab === 'emails' && renderEmailsTab()}
      </div>
    </div>
  )
}
