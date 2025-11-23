import type { ChangeEvent, FormEvent, JSX } from 'react'

type SettingsForm = {
  locale: string
  timezone: string
  marketingOptIn: boolean
  reportEmailOptIn: boolean
}

type SettingsPanelProps = {
  token: string | null
  settingsForm: SettingsForm
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onChangeLocale: (event: ChangeEvent<HTMLInputElement>) => void
  onChangeTimezone: (event: ChangeEvent<HTMLInputElement>) => void
  onToggleMarketing: (event: ChangeEvent<HTMLInputElement>) => void
  onToggleReportEmail: (event: ChangeEvent<HTMLInputElement>) => void
  message: JSX.Element | null
  isLoading: boolean
}

export function SettingsPanel({
  token,
  settingsForm,
  onSubmit,
  onChangeLocale,
  onChangeTimezone,
  onToggleMarketing,
  onToggleReportEmail,
  message,
  isLoading,
}: SettingsPanelProps) {
  const disabled = !token || isLoading

  return (
    <>
      <h2>Inställningar</h2>
      {message}
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          Språk (locale)
          <input
            type="text"
            name="settingsLocale"
            placeholder="sv-SE"
            value={settingsForm.locale}
            onChange={onChangeLocale}
            disabled={disabled}
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
            onChange={onChangeTimezone}
            disabled={disabled}
            required
          />
        </label>
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
