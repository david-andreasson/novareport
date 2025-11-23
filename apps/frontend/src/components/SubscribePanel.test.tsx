import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { PaymentState } from '../App'
import { SubscribePanel } from './SubscribePanel'

describe('SubscribePanel', () => {
  const baseState: PaymentState = {
    phase: 'idle',
    selectedPlan: null,
    payment: null,
    error: undefined,
  }

  it('visar plan-kort när phase är idle', () => {
    render(
      <SubscribePanel
        paymentState={baseState}
        message={null}
        onSelectPlan={() => {}}
        onNavigateToReport={() => {}}
        onResetPayment={() => {}}
        onCopyPaymentAddress={() => {}}
      />,
    )

    expect(screen.getByText('Månad')).toBeInTheDocument()
    expect(screen.getByText('År')).toBeInTheDocument()
  })

  it('anropar onSelectPlan när användaren väljer plan', () => {
    const handleSelectPlan = vi.fn()

    render(
      <SubscribePanel
        paymentState={baseState}
        message={null}
        onSelectPlan={handleSelectPlan}
        onNavigateToReport={() => {}}
        onResetPayment={() => {}}
        onCopyPaymentAddress={() => {}}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Välj månad' }))
    fireEvent.click(screen.getByRole('button', { name: 'Välj år' }))

    expect(handleSelectPlan).toHaveBeenCalledWith('monthly')
    expect(handleSelectPlan).toHaveBeenCalledWith('yearly')
  })

  it('visar betalningsdetaljer och anropar onCopyPaymentAddress', () => {
    const handleCopy = vi.fn()
    const state: PaymentState = {
      phase: 'pending',
      selectedPlan: 'monthly',
      payment: {
        paymentId: 'p1',
        paymentAddress: 'address123',
        amountXmr: '0.01',
        expiresAt: '2024-01-01T00:00:00Z',
      },
      error: undefined,
    }

    render(
      <SubscribePanel
        paymentState={state}
        message={null}
        onSelectPlan={() => {}}
        onNavigateToReport={() => {}}
        onResetPayment={() => {}}
        onCopyPaymentAddress={handleCopy}
      />,
    )

    expect(screen.getByText('Skicka betalning')).toBeInTheDocument()
    expect(screen.getByText(/0\.01/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Kopiera' }))
    expect(handleCopy).toHaveBeenCalledWith('address123')
  })

  it('visar success-panel och anropar onNavigateToReport', () => {
    const handleNavigate = vi.fn()
    const state: PaymentState = { ...baseState, phase: 'confirmed' }

    render(
      <SubscribePanel
        paymentState={state}
        message={null}
        onSelectPlan={() => {}}
        onNavigateToReport={handleNavigate}
        onResetPayment={() => {}}
        onCopyPaymentAddress={() => {}}
      />,
    )

    expect(screen.getByText('✓ Betalning bekräftad!')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Visa rapporter' }))
    expect(handleNavigate).toHaveBeenCalledTimes(1)
  })

  it('visar expired-panel och anropar onResetPayment', () => {
    const handleReset = vi.fn()
    const state: PaymentState = { ...baseState, phase: 'expired' }

    render(
      <SubscribePanel
        paymentState={state}
        message={null}
        onSelectPlan={() => {}}
        onNavigateToReport={() => {}}
        onResetPayment={handleReset}
        onCopyPaymentAddress={() => {}}
      />,
    )

    expect(screen.getByText('Betalningen gick ut')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Försök igen' }))
    expect(handleReset).toHaveBeenCalledTimes(1)
  })

  it('visar error-panel och anropar onResetPayment', () => {
    const handleReset = vi.fn()
    const state: PaymentState = { ...baseState, phase: 'error', error: 'Något gick fel' }

    render(
      <SubscribePanel
        paymentState={state}
        message={null}
        onSelectPlan={() => {}}
        onNavigateToReport={() => {}}
        onResetPayment={handleReset}
        onCopyPaymentAddress={() => {}}
      />,
    )

    expect(screen.getByText('Något gick fel')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Försök igen' }))
    expect(handleReset).toHaveBeenCalledTimes(1)
  })
})
