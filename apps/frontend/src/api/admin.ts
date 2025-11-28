import type { SubscriptionDetail } from './subscriptions'

export type AccountsAdminLoginMetrics = {
  success: number
  invalidCredentials: number
  error: number
  meanLatencySuccessMs: number
  meanLatencyInvalidCredentialsMs: number
  meanLatencyErrorMs: number
}

export type AccountsAdminMetrics = {
  totalUsers: number
  activeUsers: number
  logins: AccountsAdminLoginMetrics
}

export type AdminUserSettings = {
  locale: string
  timezone: string
  marketingOptIn: boolean
  reportEmailOptIn: boolean
  twoFactorEnabled: boolean
}

export type AdminUserDetails = {
  id: string
  email: string
  firstName: string | null
  lastName: string | null
  role: string
  active: boolean
  createdAt: string
  settings: AdminUserSettings
}

export type NotificationsWelcomeMetrics = {
  success: number
  error: number
}

export type NotificationsDailyEmailMetrics = {
  success: number
  noReport: number
  alreadySent: number
  noSubscribers: number
  noActiveSubscribers: number
  missingApiKey: number
  error: number
  recipientsTotal: number
}

export type NotificationsAdminMetrics = {
  welcomeEmails: NotificationsWelcomeMetrics
  dailyEmails: NotificationsDailyEmailMetrics
  latestReportDate: string | null
  latestReportEmailSent: boolean
}

export type SubscriptionsAdminMetrics = {
  activeSubscriptions: number
  activatedSuccess: number
  activatedError: number
}

export type PaymentsAdminCreatedMetrics = {
  success: number
  error: number
  meanLatencyMs: number
}

export type PaymentsAdminConfirmedMetrics = {
  success: number
  invalidState: number
  error: number
  meanLatencyMs: number
  meanTimeToConfirmSeconds: number
}

export type PaymentsAdminMetrics = {
  created: PaymentsAdminCreatedMetrics
  confirmed: PaymentsAdminConfirmedMetrics
}

export type AdminSubscriptionDetail = SubscriptionDetail

export type AdminPayment = {
  id: string
  userId: string
  status: 'PENDING' | 'CONFIRMED' | 'FAILED'
  createdAt: string
  confirmedAt: string | null
  plan: string
  amountXmr: string
}

export type DiscordAdminInfo = {
  configured: boolean
  inviteUrl: string | null
}

const authHeaders = (token: string) => ({
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
})

export async function getAccountsAdminMetrics(token: string): Promise<AccountsAdminMetrics> {
  const response = await fetch('/api/accounts/admin/metrics', {
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta admin-metrics (accounts)')
  }

  return (await response.json()) as AccountsAdminMetrics
}

export async function findUserByEmail(token: string, email: string): Promise<AdminUserDetails> {
  const params = new URLSearchParams({ email })
  const response = await fetch(`/api/accounts/admin/users/by-email?${params.toString()}`, {
    headers: authHeaders(token),
  })

  if (response.status === 404) {
    throw new Error('Ingen användare hittades med den e-postadressen.')
  }

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta användare')
  }

  return (await response.json()) as AdminUserDetails
}

export async function anonymizeUser(token: string, userId: string): Promise<AdminUserDetails> {
  const response = await fetch(`/api/accounts/admin/users/${userId}/anonymize`, {
    method: 'POST',
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte anonymisera användare')
  }

  return (await response.json()) as AdminUserDetails
}

export async function updateUserSettingsAdmin(
  token: string,
  userId: string,
  settings: AdminUserSettings,
): Promise<AdminUserDetails> {
  const response = await fetch(`/api/accounts/admin/users/${userId}/settings`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify(settings),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte uppdatera användarinställningar')
  }

  return (await response.json()) as AdminUserDetails
}

export async function getNotificationsAdminMetrics(token: string): Promise<NotificationsAdminMetrics> {
  const response = await fetch('/api/notifications/admin/metrics', {
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta admin-metrics (notifications)')
  }

  return (await response.json()) as NotificationsAdminMetrics
}

export async function resendWelcomeEmail(
  token: string,
  email: string,
  firstName: string | null,
): Promise<void> {
  const response = await fetch('/api/notifications/admin/welcome-email/resend', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ email, firstName }),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte skicka välkomstmail igen')
  }
}

export async function sendTestDailyReportEmail(
  token: string,
  email: string,
  reportDate?: string,
): Promise<void> {
  const response = await fetch('/api/notifications/admin/daily-report/test-send', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ email, reportDate: reportDate ?? null }),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte skicka testrapport')
  }
}

export async function getSubscriptionsAdminMetrics(token: string): Promise<SubscriptionsAdminMetrics> {
  const response = await fetch('/api/subscriptions/admin/metrics', {
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta admin-metrics (subscriptions)')
  }

  return (await response.json()) as SubscriptionsAdminMetrics
}

export async function getUserSubscriptionAdmin(
  token: string,
  userId: string,
): Promise<AdminSubscriptionDetail | null> {
  const response = await fetch(`/api/subscriptions/admin/users/${userId}`, {
    headers: authHeaders(token),
  })

  if (response.status === 404) {
    return null
  }

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta prenumeration för användare')
  }

  return (await response.json()) as AdminSubscriptionDetail
}

export async function getPaymentsAdminMetrics(token: string): Promise<PaymentsAdminMetrics> {
  const response = await fetch('/api/payments/admin/metrics', {
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta admin-metrics (payments)')
  }

  return (await response.json()) as PaymentsAdminMetrics
}

export async function getUserLastPaymentAdmin(
  token: string,
  userId: string,
): Promise<AdminPayment | null> {
  const response = await fetch(`/api/payments/admin/users/${userId}/last-payment`, {
    headers: authHeaders(token),
  })

  if (response.status === 404) {
    return null
  }

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta senaste betalning för användare')
  }

  return (await response.json()) as AdminPayment
}

export async function runDailyReportNow(token: string): Promise<void> {
  const response = await fetch('/api/notifications/admin/daily-report/run-now', {
    method: 'POST',
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte köra daglig rapport nu')
  }
}

export async function getDiscordAdminInfo(token: string): Promise<DiscordAdminInfo> {
  const response = await fetch('/api/notifications/admin/discord/invite', {
    headers: authHeaders(token),
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || 'Kunde inte hämta Discord-info')
  }

  return (await response.json()) as DiscordAdminInfo
}
