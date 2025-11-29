import type { JSX } from 'react'

import type { UserProfile, SubscriptionState } from '../App'

type ProfilePanelProps = {
  token: string | null
  isLoadingProfile: boolean
  message: JSX.Element | null
  profile: UserProfile | null
  subscriptionState: SubscriptionState
  onRefresh: () => void
  formatDateTime: (iso: string) => string
  translateStatus: (status: string) => string
  onRequestDiscordInvite: () => void
}

export function ProfilePanel({
  token,
  isLoadingProfile,
  message,
  profile,
  subscriptionState,
  onRefresh,
  formatDateTime,
  translateStatus,
  onRequestDiscordInvite,
}: ProfilePanelProps) {
  if (!token) {
    return <p className="auth-note">Logga in för att se din profil.</p>
  }

  return (
    <>
      <div className="panel-actions">
        <button
          className="pill-button"
          type="button"
          onClick={onRefresh}
          disabled={isLoadingProfile}
        >
          {isLoadingProfile ? 'Uppdaterar…' : '↻ Uppdatera status'}
        </button>
      </div>
      {message}
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
        isLoadingProfile ? null : (
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
            <>
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
              <div className="panel-actions">
                <button
                  type="button"
                  className="pill-button"
                  onClick={onRequestDiscordInvite}
                >
                  Skicka Discord-invite till min e-post
                </button>
              </div>
            </>
          ) : (
            <p className="auth-note">Ingen aktiv prenumeration.</p>
          )
        )}

        {subscriptionState.phase === 'idle' && (
          <p className="auth-note">Prenumerationsstatus hämtas efter inloggning.</p>
        )}
      </section>
    </>
  )
}
