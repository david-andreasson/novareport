import type { ChangeEvent, FormEvent, JSX } from 'react'

type LoginForm = {
  email: string
  password: string
}

type LoginPanelProps = {
  loginForm: LoginForm
  onChange: (form: LoginForm) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  message: JSX.Element | null
  isLoading: boolean
}

export function LoginPanel({ loginForm, onChange, onSubmit, message, isLoading }: LoginPanelProps) {
  const handleEmailChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...loginForm, email: event.target.value })
  }

  const handlePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...loginForm, password: event.target.value })
  }

  return (
    <>
      <h2>Logga in</h2>
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          E-post
          <input
            type="email"
            name="loginEmail"
            placeholder="namn@example.com"
            value={loginForm.email}
            onChange={handleEmailChange}
            required
          />
        </label>
        <label>
          Lösenord
          <input
            type="password"
            name="loginPassword"
            placeholder="••••••"
            value={loginForm.password}
            onChange={handlePasswordChange}
            required
          />
        </label>
        {message}
        <button className="pill-button" type="submit" disabled={isLoading}>
          {isLoading ? 'Arbetar…' : 'Fortsätt'}
        </button>
      </form>
    </>
  )
}
