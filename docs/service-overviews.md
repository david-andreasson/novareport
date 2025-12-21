# NovaReport – Service-översikt

Jag använder den här filen som en kort översikt över de viktigaste tjänsterna i NovaReport-systemet. För detaljerad implementation går jag till respektive tjänsts README/dokumentation.

---

## accounts-service

**Ansvar:**
- Registrering och login
- Utgivning av JWT-access tokens
- Hantering av grundläggande användarprofil och email-inställningar (t.ex. om användaren vill ha rapport-email)

**Nyckelkoncept:**
- Endpoints under `/auth` för registrering/login
- `JwtService` som skapar och validerar tokens enligt `docs/jwt-conventions.md`
- Internt API för att hämta rapport-email-prenumeranter (används av `notifications-service`)

---

## subscriptions-service

**Ansvar:**
- Hålla reda på vilka användare som har aktiva prenumerationer
- Erbjuda ett enkelt API för "har den här användaren access just nu?"

**Nyckelkoncept:**
- Publikt API:
  - `GET /api/v1/subscriptions/me/has-access` – används av frontend för att avgöra om rapporter får läsas
- Internt API:
  - `POST /api/v1/internal/subscriptions/activate` – aktiverar prenumeration efter lyckad betalning (anropas av `payments-xmr-service`)
- Dev-flagga `SUBS_FAKE_ALL_ACTIVE` för att göra alla användare aktiva i utvecklingsmiljö.

---

## payments-xmr-service

**Ansvar:**
- Hantera Monero-betalningar (stagenet i nuläget)
- Skapa betalningar med unika Monero-subadresser
- Övervaka inkommande transaktioner och bekräfta betalningar
- Aktivera prenumerationer via `subscriptions-service`

**Nyckelkoncept:**
- Publikt API för att:
  - Skapa betalning (`POST /api/v1/payments/create`)
  - Läsa status (`GET /api/v1/payments/{id}/status`)
- Internt API för manuell/administrativ bekräftelse:
  - `POST /api/v1/internal/payments/{id}/confirm`
- `MoneroWalletClient` som pratar med `monero-wallet-rpc`
- `PaymentMonitorService` som periodiskt kollar saldo per subadress och bekräftar betalningar automatiskt
- Se `docs/payments-xmr-service-guide.md` för en mer komplett genomgång.

---

## payments-stripe-service

**Ansvar:**
- Hantera kortbetalningar med Stripe PaymentIntents
- Skapa PaymentIntent och visa status för frontend
- Konsumera Stripe-webhooks för att bekräfta eller avvisa betalningar
- Aktivera prenumerationer via `subscriptions-service` efter lyckad betalning
- Skicka betalningsbekräftelse-mail via `notifications-service`

**Nyckelkoncept:**
- Publikt API under `/api/v1/payments-stripe`:
  - `POST /create-intent` – skapar PaymentIntent och returnerar `clientSecret`
  - `GET /{paymentId}/status` – läser status för en betalning
- Webhook-endpoint `/api/v1/payments-stripe/webhook/stripe` verifierar Stripe-signatur och uppdaterar betalningar.
- `SubscriptionActivationService` + `SubscriptionsClient` aktiverar access när betalningen är klar
- `PaymentEventPublisher` publicerar events när betalningar bekräftas

---

## reporter-service

**Ansvar:**
- Hämta in nyheter (RSS/News API m.m.)
- Deduplicera och lagra `NewsItem`
- Generera rapporter med hjälp av AI (1min.ai)
- Schemalägga rapportgenerering
- Skicka "rapport är klar"-notiser till `notifications-service`

**Nyckelkoncept:**
- Scheduler (`ScheduledReportGenerator`) som kör var 4:e timme
- AI-integration via `OneMinAiSummarizerService` (struktur: Executive Summary, Key Developments, Market Trends, Outlook)
- Intern klient (`NotificationsClient`) som anropar `/api/v1/internal/notifications/report-ready` i `notifications-service`
- Se `docs/onemin-ai-integration.md` och `docs/scheduler-implementation.md` för detaljer.

---

## notifications-service

**Ansvar:**
- Lagra färdiga rapporter för notifiering (`NotificationReport`)
- Skicka rapporter via email (dagliga rapportmail + välkomstmail)
- Skicka rapporter till Discord (embeds)
- Skicka mail när betalning bekräftas (Stripe eller XMR)
- Exponera ett enkelt API för att hämta senaste rapporten

**Nyckelkoncept:**
- Internt API:
  - `POST /api/v1/internal/notifications/report-ready`
  - `POST /api/v1/internal/notifications/welcome-email`
  - `POST /api/v1/internal/notifications/payment-confirmed-email`
- Publikt API: `GET /api/v1/notifications/latest`
- `DailyReportEmailJob` för dagliga email-utskick
- `DiscordReportService` + `DailyReportDiscordJob` för Discord-notiser
- `NotificationReportService` sparar rapporter och används även av frontend
- Konfiguration via `notifications.mail.*` och `notifications.discord.*`
- Se `docs/notifications-service.md` för en mer detaljerad beskrivning.

---

## frontend

**Ansvar:**
- Ge användaren ett UI för hela flödet:
  - registrering/login
  - köpa prenumeration med Monero eller Stripe
  - visa om användaren har access
  - läsa senaste rapporten

**Nyckelkoncept:**
- Byggd med React + TypeScript + Vite
- Pratar med backend-tjänsterna via `VITE_*`-konfigurerade bas-URL:er (se frontend-README)
- Stripe-flödet använder PaymentIntent `clientSecret` som hämtas via `payments-stripe-service`
- Visar rapportsammanfattningen med enkel markdown-liknande formattering (rubriker m.m.)

---

Jag använder den här översikten som en snabb "mentalkarta" över systemet. För mer djupgående instruktioner går jag till respektive dokumentation i `docs/` och under varje apps katalog.
