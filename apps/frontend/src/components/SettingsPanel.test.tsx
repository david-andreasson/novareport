import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SettingsPanel } from './SettingsPanel'

describe('SettingsPanel', () => {
  const baseForm = {
    marketingOptIn: false,
    reportEmailOptIn: false,
  }

  it('renders checkboxes and save button with correct labels', () => {
    render(
      <SettingsPanel
        token="token"
        settingsForm={baseForm}
        onSubmit={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    expect(screen.getByText('Ta emot nyheter och uppdateringar')).toBeInTheDocument()
    expect(screen.getByText('Ta emot daglig rapport via e-post')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Spara inställningar' })).toBeInTheDocument()
  })

  it('disables all fields and shows notice when token is missing', () => {
    render(
      <SettingsPanel
        token={null}
        settingsForm={baseForm}
        onSubmit={() => {}}
        onToggleMarketing={() => {}}
        onToggleReportEmail={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    const marketingCheckbox = screen.getByLabelText('Ta emot nyheter och uppdateringar') as HTMLInputElement
    const reportCheckbox = screen.getByLabelText('Ta emot daglig rapport via e-post') as HTMLInputElement

    expect(marketingCheckbox).toBeDisabled()
    expect(reportCheckbox).toBeDisabled()
    expect(screen.getByText('Logga in för att kunna spara.')).toBeInTheDocument()
  })

  it('calls onSubmit when form is submitted', () => {
    const handleSubmit = vi.fn((event: React.FormEvent<HTMLFormElement>) => event.preventDefault())

    render(
      <SettingsPanel
        token="token"
        settingsForm={baseForm}
        onSubmit={handleSubmit}
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
