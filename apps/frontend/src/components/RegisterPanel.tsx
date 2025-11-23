import type { ChangeEvent, FormEvent, JSX } from 'react'

type RegisterForm = {
  email: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
}

type RegisterPanelProps = {
  registerForm: RegisterForm
  onChange: (form: RegisterForm) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  showPasswordFeedback: boolean
  meetsPasswordPolicy: boolean
  passwordsMatch: boolean
  message: JSX.Element | null
  isLoading: boolean
}

export function RegisterPanel({
  registerForm,
  onChange,
  onSubmit,
  showPasswordFeedback,
  meetsPasswordPolicy,
  passwordsMatch,
  message,
  isLoading,
}: RegisterPanelProps) {
  const handleChange = (field: keyof RegisterForm) => (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...registerForm, [field]: event.target.value })
  }

  return (
    <>
      <h2>Skapa konto</h2>
      <form className="auth-form" onSubmit={onSubmit}>
        <label>
          Förnamn
          <input
            type="text"
            name="registerFirstName"
            placeholder="Anna"
            value={registerForm.firstName}
            onChange={handleChange('firstName')}
            required
          />
        </label>
        <label>
          Efternamn
          <input
            type="text"
            name="registerLastName"
            placeholder="Svensson"
            value={registerForm.lastName}
            onChange={handleChange('lastName')}
            required
          />
        </label>
        <label>
          E-post
          <input
            type="email"
            name="registerEmail"
            placeholder="namn@example.com"
            value={registerForm.email}
            onChange={handleChange('email')}
            required
          />
        </label>
        <label>
          Lösenord
          <input
            type="password"
            name="registerPassword"
            placeholder="Minst 8 tecken"
            value={registerForm.password}
            onChange={handleChange('password')}
            required
          />
        </label>
        <label>
          Bekräfta lösenord
          <input
            type="password"
            name="registerConfirmPassword"
            placeholder="Upprepa lösenord"
            value={registerForm.confirmPassword}
            onChange={handleChange('confirmPassword')}
            required
          />
        </label>
        {showPasswordFeedback && (
          <ul className="password-feedback">
            <li className={meetsPasswordPolicy ? 'valid' : 'invalid'}>
              {meetsPasswordPolicy ? '✓' : '✗'} Uppfyller lösenordskraven (minst 8 tecken, stor bokstav och specialtecken)
            </li>
            <li className={passwordsMatch ? 'valid' : 'invalid'}>
              {passwordsMatch ? '✓' : '✗'} Lösenorden matchar
            </li>
          </ul>
        )}
        {message}
        <button
          className="pill-button"
          type="submit"
          disabled={isLoading || !meetsPasswordPolicy || !passwordsMatch}
        >
          {isLoading ? 'Skapar…' : 'Registrera'}
        </button>
      </form>
    </>
  )
}
