import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SettingsPanel } from './SettingsPanel'

describe('SettingsPanel', () => {
  const baseForm = {
    locale: 'sv-SE',
    timezone: 'Europe/Stockholm',
    marketingOptIn: false,
    reportEmailOptIn: false,
  }

  it('renders all settings fields with correct labels', () => {
    render(
      <SettingsPanel
        token="token"
        settingsForm={baseForm}
        onSubmit={() => {}}
        onChangeLocale={() => {}}
        onChangeTimezone={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    expect(screen.getByLabelText('Språk (locale)')).toBeInTheDocument()
    expect(screen.getByLabelText('Tidszon')).toBeInTheDocument()
    expect(screen.getByText('Ta emot nyheter och uppdateringar')).toBeInTheDocument()
    expect(screen.getByText('Ta emot daglig rapport via e-post')).toBeInTheDocument()
  })

  it('disables all fields and shows notice when token is missing', () => {
    render(
      <SettingsPanel
        token={null}
        settingsForm={baseForm}
        onSubmit={() => {}}
        onChangeLocale={() => {}}
        onChangeTimezone={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    const localeInput = screen.getByLabelText('Språk (locale)') as HTMLInputElement
    const timezoneInput = screen.getByLabelText('Tidszon') as HTMLInputElement

    expect(localeInput).toBeDisabled()
    expect(timezoneInput).toBeDisabled()
    expect(screen.getByText('Logga in för att kunna spara.')).toBeInTheDocument()
  })

  it('calls onSubmit when form is submitted', () => {
    const handleSubmit = vi.fn((event: React.FormEvent<HTMLFormElement>) => event.preventDefault())

    render(
      <SettingsPanel
        token="token"
        settingsForm={baseForm}
        onSubmit={handleSubmit}
        onChangeLocale={() => {}}
        onChangeTimezone={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    fireEvent.submit(screen.getByRole('button', { name: 'Spara inställningar' }))

    expect(handleSubmit).toHaveBeenCalledTimes(1)
  })

  it('shows loading state on button when isLoading is true', () => {
    render(
      <SettingsPanel
        token="token"
        settingsForm={baseForm}
        onSubmit={() => {}}
        onChangeLocale={() => {}}
        onChangeTimezone={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={true}
      />,
    )

    const button = screen.getByRole('button') as HTMLButtonElement
    expect(button).toBeDisabled()
    expect(button).toHaveTextContent('Sparar…')
  })
})
