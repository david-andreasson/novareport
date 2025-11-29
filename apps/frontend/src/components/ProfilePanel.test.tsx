import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import type { SubscriptionState, UserProfile } from '../App'
import { ProfilePanel } from './ProfilePanel'

describe('ProfilePanel', () => {
  const baseSubscriptionState: SubscriptionState = {
    phase: 'idle',
    hasAccess: null,
    detail: null,
  }

  const formatDateTime = vi.fn((iso: string) => `formatted-${iso}`)
  const translateStatus = vi.fn((status: string) => `translated-${status}`)
  const onRequestDiscordInvite = vi.fn()

  it('shows login hint when token is missing', () => {
    render(
      <ProfilePanel
        token={null}
        isLoadingProfile={false}
        message={null}
        profile={null}
        subscriptionState={baseSubscriptionState}
        onRefresh={() => {}}
        formatDateTime={formatDateTime}
        translateStatus={translateStatus}
        onRequestDiscordInvite={onRequestDiscordInvite}
      />,
    )

    expect(screen.getByText('Logga in för att se din profil.')).toBeInTheDocument()
  })

  it('renders profile information when profile is present', () => {
    const profile: UserProfile = {
      id: 'user-1',
      email: 'user@example.com',
      firstName: 'Anna',
      lastName: 'Svensson',
      role: 'USER',
    }

    render(
      <ProfilePanel
        token="token"
        isLoadingProfile={false}
        message={null}
        profile={profile}
        subscriptionState={baseSubscriptionState}
        onRefresh={() => {}}
        formatDateTime={formatDateTime}
        translateStatus={translateStatus}
        onRequestDiscordInvite={onRequestDiscordInvite}
      />,
    )

    expect(screen.getByText('Anna Svensson')).toBeInTheDocument()
    expect(screen.getByText('user@example.com')).toBeInTheDocument()
  })

  it('renders subscription information when access is active', () => {
    const subscriptionState: SubscriptionState = {
      phase: 'success',
      hasAccess: true,
      detail: {
        userId: 'user-1',
        plan: 'monthly',
        status: 'ACTIVE',
        startAt: '2024-01-01T00:00:00Z',
        endAt: '2024-02-01T00:00:00Z',
      },
    }

    render(
      <ProfilePanel
        token="token"
        isLoadingProfile={false}
        message={null}
        profile={null}
        subscriptionState={subscriptionState}
        onRefresh={() => {}}
        formatDateTime={formatDateTime}
        translateStatus={translateStatus}
        onRequestDiscordInvite={onRequestDiscordInvite}
      />,
    )

    expect(screen.getByText('Plan')).toBeInTheDocument()
    expect(screen.getByText('monthly')).toBeInTheDocument()
    expect(translateStatus).toHaveBeenCalledWith('ACTIVE')
    expect(formatDateTime).toHaveBeenCalledWith('2024-01-01T00:00:00Z')
    expect(formatDateTime).toHaveBeenCalledWith('2024-02-01T00:00:00Z')
  })

  it('calls onRefresh when refresh button is clicked', () => {
    const handleRefresh = vi.fn()

    render(
      <ProfilePanel
        token="token"
        isLoadingProfile={false}
        message={null}
        profile={null}
        subscriptionState={baseSubscriptionState}
        onRefresh={handleRefresh}
        formatDateTime={formatDateTime}
        translateStatus={translateStatus}
        onRequestDiscordInvite={onRequestDiscordInvite}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: '↻ Uppdatera status' }))

    expect(handleRefresh).toHaveBeenCalledTimes(1)
  })
})
