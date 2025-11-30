import type { ChangeEvent, FormEvent, JSX } from 'react'

export type SettingsForm = {
  marketingOptIn: boolean
  reportEmailOptIn: boolean
}

type SettingsPanelProps = {
  token: string | null
  settingsForm: SettingsForm
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onToggleMarketing: (event: ChangeEvent<HTMLInputElement>) => void
  onToggleReportEmail: (event: ChangeEvent<HTMLInputElement>) => void
  message: JSX.Element | null
  isLoading: boolean
}

export function SettingsPanel({
  token,
  settingsForm,
  onSubmit,
  onToggleMarketing,
  onToggleReportEmail,
  message,
  isLoading,
}: SettingsPanelProps) {
  const disabled = !token || isLoading

  return (
    <>
      {message}
      <form className="auth-form" onSubmit={onSubmit}>
        <label className="checkbox-row">
          <input
            type="checkbox"
            name="settingsMarketing"
            checked={settingsForm.marketingOptIn}
            onChange={onToggleMarketing}
            disabled={disabled}
          />
          <span>Ta emot nyheter och uppdateringar</span>
        </label>
        <label className="checkbox-row">
          <input
            type="checkbox"
            name="settingsReportEmail"
            checked={settingsForm.reportEmailOptIn}
            onChange={onToggleReportEmail}
            disabled={disabled}
          />
          <span>Ta emot daglig rapport via e-post</span>
        </label>
        <button className="pill-button" type="submit" disabled={disabled}>
          {isLoading ? 'Sparar…' : 'Spara inställningar'}
        </button>
      </form>
      {!token && <p className="auth-note">Logga in för att kunna spara.</p>}
    </>
  )
}
