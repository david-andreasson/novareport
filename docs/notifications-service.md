# notifications-service – Email & Discord-notifieringar

**Syfte:** Hantera utskick av NovaReport-rapporter via **email** och **Discord**.

Den här tjänsten tar emot färdiga rapporter från `reporter-service`, lagrar dem som `NotificationReport` och ser till att de skickas till användare beroende på kanal.

---

## Översikt

- Tar emot "rapport är klar"-notiser från `reporter-service` via ett internt API.
- Sparar rapporter i tabellen `notification_reports`.
- Skickar rapporter:
  - **Email** – dagligt utskick till aktiva prenumeranter med email-notiser aktiverade.
  - **Discord** – utskick till en Discord-kanal via webhook, normalt när en ny rapport genereras (med fallback-jobb).
- Håller koll på när rapporter har skickats via fälten `emailSentAt` och `discordSentAt`.

---

## Datamodell: NotificationReport

Entiteten `NotificationReport` representerar den rapport som ska notifieras ut:

- `id` – internt UUID
- `reportId` – ID för rapporten i `reporter-service` (UUID, unik)
- `reportDate` – datum för rapporten (`LocalDate`, unik)
- `summary` – AI-genererad rapportsammanfattning (`TEXT`)
- `createdAt` / `updatedAt` – audit-fält
- `emailSentAt` – tidpunkt när email-utskicket genomfördes (eller `null` om ej skickad)
- `discordSentAt` – tidpunkt när Discord-utskicket genomfördes (eller `null` om ej skickad)
- `telegramSentAt` – reserverat för framtida Telegram-integration

Index:
- Unik index på `report_date` och `report_id` för att undvika dubletter per dag/rapport.

---

## API

### 1. Internt API: rapport klar

När `reporter-service` har genererat en rapport, anropar den:

- **Metod:** `POST /api/v1/internal/notifications/report-ready`
- **Body:** `ReportReadyNotificationRequest` (från `reporter-service`)
  - `reportId` – UUID för rapporten
  - `reportDate` – datum för rapporten
  - `summary` – AI-sammanfattningen (text)

Flöde i `NotificationReportService.upsertReport(...)`:

1. Försöker hitta befintlig `NotificationReport` på `reportId` eller `reportDate`.
2. Skapar/uppdaterar posten med `reportId`, `reportDate` och `summary`.
3. Sparar posten.
4. Försöker skicka rapporten till Discord via `DiscordReportService.sendDailyReport(...)`.
   - Vid lyckat skick:
     - Sätter `discordSentAt` till `Instant.now()` och sparar igen.

Detta ger en **nära realtids**-push till Discord när en ny rapport är klar, givet att Discord-notifieringar är aktiverade.

### 2. Publikt API: senaste rapporten

- **Metod:** `GET /api/v1/notifications/latest`
- **Svar:** `NotificationReportResponse`
  - `reportId`
  - `reportDate`
  - `summary`
  - `createdAt`
  - `updatedAt`

Detta används av frontend för att hämta den senaste rapporten (oavsett kanal).

---

## Email-notiser

Email-flödet sköts av `DailyReportEmailJob` och `ReportEmailService`.

### Schema

- Cron-uttryck: `notifications.email.cron`  
  Default: `0 0 6 * * *` (kl 06:00 varje dag, `Europe/Stockholm`).

### Flöde

1. Jobbet beräknar "idag" utifrån `notifications.report.zone` (default `Europe/Stockholm`).
2. Hämtar `NotificationReport` för dagens datum.
3. Om `emailSentAt` redan är satt för den rapporten → loggar och hoppar över.
4. Hämtar email-prenumeranter från `accounts-service` (via `AccountsClient`).
5. Hämtar aktiva användar-ID:n från `subscriptions-service` (via `SubscriptionsClient`).
6. Filtrerar ner till de prenumeranter som både:
   - har email-notiser aktiverade, och
   - har en **aktiv prenumeration** just nu.
7. För varje mottagare:
   - `ReportEmailService.sendDailyReport(email, report)` skickar ett enkelt textmail:
     - Ämne: `Nova Report – Daily report <YYYY-MM-DD>`
     - Body: hälsning + rapportsammanfattning.
8. Om minst ett mail skickats:
   - Sätter `emailSentAt` på rapporten till `Instant.now()` och sparar.

### Konfiguration

I `application-prod.properties` och `docker-compose.prod.yml`:

- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`
- `NOTIFICATIONS_MAIL_FROM` → `notifications.mail.from`
- `NOTIFICATIONS_REPORT_ZONE` → `notifications.report.zone`
- `NOTIFICATIONS_EMAIL_CRON` → `notifications.email.cron`

---

## Discord-notiser

Discord-flödet sköts av `DiscordReportService` och `DailyReportDiscordJob`.

### Aktivering

- `notifications.discord.enabled` (bool) – styr om Discord-notiser är påslagna.
- `notifications.discord.webhook-url` – Discord webhook URL.
- `notifications.discord.cron` – schema för fallback-jobbet (default `0 5 6 * * *`).

I Docker (prod):

- `NOTIFICATIONS_DISCORD_ENABLED` → `notifications.discord.enabled`
- `NOTIFICATIONS_DISCORD_WEBHOOK_URL` → `notifications.discord.webhook-url`
- `NOTIFICATIONS_DISCORD_CRON` → `notifications.discord.cron`

### Flöde 1: Omedelbart utskick vid rapport-klar

1. `NotificationReportService.upsertReport(...)` skapar/uppdaterar en `NotificationReport`.
2. Anropar `DiscordReportService.sendDailyReport(report)`.
3. `DiscordReportService` bygger en Discord-embed payload med:
   - Huvudtitel: `"Nova Report – 4 Hour Report <YYYY-MM-DD>"`.
   - Uppdelning av `summary` i sektioner baserat på rubriker från AI:n:
     - "Executive Summary"
     - "Key Developments"
     - "Market Trends"
     - "Outlook"
   - Varje sektion skickas som en egen embed (max 4 embeds per rapport).
   - Markdown-rubriker (`##`, `1. Executive Summary` osv) konverteras till **fetstil** i Discord-beskrivningen.
   - Längden på varje embed-beskrivning begränsas till 4096 tecken (Discord-gräns).
4. Om anropet mot Discord lyckas:
   - `discordSentAt` sätts på rapporten och sparas.

### Flöde 2: Fallback-jobb (DailyReportDiscordJob)

Ett schemalagt jobb fungerar som säkerhetsnät:

1. Körs enligt `notifications.discord.cron` (default **06:05** varje dag).
2. Hämtar rapporten för dagens datum.
3. Om `discordSentAt` redan är satt → loggar och hoppar över (inget dubbelutskick).
4. Om inte:
   - Försöker skicka via `DiscordReportService.sendDailyReport`.
   - Vid success sätts `discordSentAt` och rapporten sparas.

Det innebär att Discord normalt får rapporterna när de blir klara (via upsert-flödet), men fallback-jobbet täcker upp om något gick fel vid första försöket.

---

## Konfiguration sammanfattning

Viktiga properties (prod):

- **Gemensamt**
  - `internal.api-key` – intern nyckel för säkra interna anrop.
  - `accounts.base-url` – bas-URL till `accounts-service`.
  - `subs.base-url` – bas-URL till `subscriptions-service`.

- **Email**
  - `spring.mail.*` – SMTP-konfiguration.
  - `notifications.mail.from` – avsändaradress.
  - `notifications.email.cron` – cron för email-jobbet.
  - `notifications.report.zone` – tidszon för datum/cron.

- **Discord**
  - `notifications.discord.enabled` – slå på/av Discord-notiser.
  - `notifications.discord.webhook-url` – Discord-webhook.
  - `notifications.discord.cron` – cron för fallback-jobbet.

---

## Lokalt vs produktion

- **Lokal utveckling (docker-compose.yml):**
  - Notifications-service körs mot andra dev-containrar och jag kan enkelt starta allt med `docker-compose up`.
  - Email/Discord kan jag lämna avstängt om jag inte vill konfigurera riktiga SMTP/Discord-webhooks lokalt.

- **Produktion (docker-compose.prod.yml):**
  - Använder PostgreSQL i stället för H2.
  - Laddar in alla känsliga värden (JWT, INTERNAL_API_KEY, SMTP, Discord) via miljövariabler.
  - Healthchecks på `/actuator/health` används för att hålla koll på tjänstens status.

Jag använder den här filen som en praktisk referens när jag konfigurerar eller felsöker notifieringar – koden i `notifications-service` följer strukturen ovan.
