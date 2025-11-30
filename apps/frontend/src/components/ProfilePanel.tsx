import type { ChangeEvent, FormEvent, JSX } from 'react'

import type { UserProfile, SubscriptionState } from '../App'
import { SettingsPanel } from './SettingsPanel'
import type { SettingsForm } from './SettingsPanel'

type ProfilePanelProps = {
  token: string | null
  isLoadingProfile: boolean
  isSavingSettings: boolean
  profileMessage: JSX.Element | null
  settingsMessage: JSX.Element | null
  profile: UserProfile | null
  subscriptionState: SubscriptionState
  settingsForm: SettingsForm
  onRefresh: () => void
  formatDateTime: (iso: string) => string
  translateStatus: (status: string) => string
  onRequestDiscordInvite: () => void
  onSettingsSubmit: (event: FormEvent<HTMLFormElement>) => void
  onToggleMarketing: (event: ChangeEvent<HTMLInputElement>) => void
  onToggleReportEmail: (event: ChangeEvent<HTMLInputElement>) => void
}

export function ProfilePanel({
  token,
  isLoadingProfile,
  isSavingSettings,
  profileMessage,
  settingsMessage,
  profile,
  subscriptionState,
  settingsForm,
  onRefresh,
  formatDateTime,
  translateStatus,
  onRequestDiscordInvite,
  onSettingsSubmit,
  onToggleMarketing,
  onToggleReportEmail,
}: ProfilePanelProps) {
  const isAdmin = profile?.role === 'ADMIN'

  return (
    <>
      <div className="panel-actions panel-actions--split">
        <div className="panel-actions__left">
          {subscriptionState.phase === 'success' && subscriptionState.hasAccess && (
            <button
              type="button"
              className="pill-button"
              onClick={onRequestDiscordInvite}
              disabled={!token}
            >
              Skicka Discord-invite till min e-post
            </button>
          )}
        </div>
        <div className="panel-actions__right">
          <button
            className="pill-button"
            type="button"
            onClick={onRefresh}
            disabled={isLoadingProfile || !token}
          >
            {isLoadingProfile ? 'Uppdaterar…' : '↻ Uppdatera status'}
          </button>
        </div>
      </div>
      {profileMessage}
      {!token && <p className="auth-note">Logga in för att se din profil.</p>}
      {token &&
        (profile ? (
          <div className="auth-details">
            <dl>
              <dt>Namn</dt>
              <dd>
                {profile.firstName} {profile.lastName}
              </dd>
              <dt>E-post</dt>
              <dd>{profile.email}</dd>
              {isAdmin && (
                <>
                  <dt>Roll</dt>
                  <dd>{profile.role}</dd>
                  <dt>ID</dt>
                  <dd>{profile.id}</dd>
                </>
              )}
            </dl>
          </div>
        ) : (
          isLoadingProfile ? null : (
            <p className="auth-note">Ingen profildata hämtad ännu.</p>
          )
        ))}
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

      <section className="settings-section">
        <h3>Inställningar</h3>
        <SettingsPanel
          token={token}
          settingsForm={settingsForm}
          onSubmit={onSettingsSubmit}
          onToggleMarketing={onToggleMarketing}
          onToggleReportEmail={onToggleReportEmail}
          message={settingsMessage}
          isLoading={isSavingSettings}
        />
      </section>
    </>
  )
}
