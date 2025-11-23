import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { LoginPanel } from './LoginPanel'

describe('LoginPanel', () => {
  const baseForm = { email: '', password: '' }

  it('renderar fält med korrekta etiketter och placeholders', () => {
    render(
      <LoginPanel
        loginForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    expect(screen.getByLabelText('E-post')).toBeInTheDocument()
    const passwordInput = screen.getByLabelText('Lösenord') as HTMLInputElement
    expect(passwordInput).toBeInTheDocument()
    expect(passwordInput.placeholder).toBe('••••••')
  })

  it('anropar onChange när användaren skriver i fält', () => {
    const handleChange = vi.fn()

    render(
      <LoginPanel
        loginForm={baseForm}
        onChange={handleChange}
        onSubmit={() => {}}
        message={null}
        isLoading={false}
      />,
    )

    fireEvent.change(screen.getByLabelText('E-post'), {
      target: { value: 'user@example.com' },
    })

    expect(handleChange).toHaveBeenCalledWith({ email: 'user@example.com', password: '' })
  })

  it('anropar onSubmit när formuläret skickas in', () => {
    const handleSubmit = vi.fn((event: React.FormEvent<HTMLFormElement>) => event.preventDefault())

    render(
      <LoginPanel
        loginForm={baseForm}
        onChange={() => {}}
        onSubmit={handleSubmit}
        message={null}
        isLoading={false}
      />,
    )

    fireEvent.submit(screen.getByRole('button', { name: 'Fortsätt' }))

    expect(handleSubmit).toHaveBeenCalledTimes(1)
  })

  it('visar laddningstillstånd och disablar knappen', () => {
    render(
      <LoginPanel
        loginForm={baseForm}
        onChange={() => {}}
        onSubmit={() => {}}
        message={null}
        isLoading={true}
      />,
    )

    const button = screen.getByRole('button') as HTMLButtonElement
    expect(button).toBeDisabled()
    expect(button).toHaveTextContent('Arbetar…')
  })
})
