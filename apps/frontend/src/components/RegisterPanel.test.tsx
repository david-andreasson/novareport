import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { RegisterPanel } from './RegisterPanel'

describe('RegisterPanel', () => {
  const baseForm = {
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
  }

  it('renders all fields with correct labels', () => {
    render(
      <RegisterPanel
        registerForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        showPasswordFeedback={false}
        meetsPasswordPolicy={false}
        passwordsMatch={false}
        message={null}
        isLoading={false}
      />,
    )

    expect(screen.getByLabelText('Förnamn')).toBeInTheDocument()
    expect(screen.getByLabelText('Efternamn')).toBeInTheDocument()
    expect(screen.getByLabelText('E-post')).toBeInTheDocument()
    expect(screen.getByLabelText('Lösenord')).toBeInTheDocument()
    expect(screen.getByLabelText('Bekräfta lösenord')).toBeInTheDocument()
  })

  it('shows password feedback when showPasswordFeedback is true', () => {
    render(
      <RegisterPanel
        registerForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        showPasswordFeedback={true}
        meetsPasswordPolicy={false}
        passwordsMatch={false}
        message={null}
        isLoading={false}
      />,
    )

    expect(screen.getByText(/Uppfyller lösenordskraven/)).toBeInTheDocument()
    expect(screen.getByText(/Lösenorden matchar/)).toBeInTheDocument()
  })

  it('disables button if password requirements are not met or passwords do not match', () => {
    const { rerender } = render(
      <RegisterPanel
        registerForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        showPasswordFeedback={true}
        meetsPasswordPolicy={false}
        passwordsMatch={false}
        message={null}
        isLoading={false}
      />,
    )

    let button = screen.getByRole('button', { name: 'Registrera' }) as HTMLButtonElement
    expect(button).toBeDisabled()

    rerender(
      <RegisterPanel
        registerForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        showPasswordFeedback={true}
        meetsPasswordPolicy={true}
        passwordsMatch={true}
        message={null}
        isLoading={false}
      />,
    )

    button = screen.getByRole('button', { name: 'Registrera' }) as HTMLButtonElement
    expect(button).not.toBeDisabled()
  })

  it('calls onSubmit when form is submitted', () => {
    const handleSubmit = vi.fn((event: React.FormEvent<HTMLFormElement>) => event.preventDefault())

    render(
      <RegisterPanel
        registerForm={baseForm}
        onChange={() => {}}
        onSubmit={handleSubmit}
        showPasswordFeedback={false}
        meetsPasswordPolicy={true}
        passwordsMatch={true}
        message={null}
        isLoading={false}
      />,
    )

    fireEvent.submit(screen.getByRole('button', { name: 'Registrera' }))

    expect(handleSubmit).toHaveBeenCalledTimes(1)
  })
})
