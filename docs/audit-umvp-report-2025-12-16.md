# Nova Report – Thesis Readiness Audit (Dec 16, 2025)

Scope: Use the structure from docs/audit-umvp.md, but evaluate “thesis/demo readiness” (you are ~98% done) rather than UMVP compliance.

---

## English Report

### 1. Executive Summary

- Strength: Clear microservice separation and story consistent with docs/Presentation.md.
- Strength: Local quality gate exists (check-code.ps1) and last run indicates everything passed (code-quality-report.txt).
- Strength: Production compose is present with env-based configuration and healthchecks (deploy/docker-compose.prod.yml).
- Strength: Consistent use of validation and ProblemDetail-style error responses (e.g. apps/accounts-service/.../GlobalExceptionHandler.java).
- Strength: Background jobs exist for the core value (scheduled report generation + daily distribution).
- Risk: README-CODE-QUALITY.md references a workflow that does not exist in repo (.github/workflows/code-quality.yml).
- Risk: Prod healthchecks use curl in containers; runtime images may not contain curl (e.g. apps/accounts-service/Dockerfile).
- Risk: Internal API key enforcement differs between services (constant-time compare in payments-*, plain equals in others).
- Risk: External dependency for AI summarization (1min.ai) must have a demo fallback plan (apps/reporter-service/.../OneMinAiSummarizerService.java).
- Recommendation: Focus final 2% on demo runbook, environment variables, and operational reproducibility.

### 2. Scores Table (0–5)

| Area | Score | Comment |
|------|------:|---------|
| 1. Architecture Fit | 4 | Responsibilities are clearly separated across services and match the demo narrative. |
| 2. Clean Code & SOLID | 4 | Code is generally clean; some cross-service inconsistencies remain. |
| 3. REST & Spring Conventions | 4 | Controllers/DTO/validation are in place; minor base-path inconsistencies. |
| 4. Security & Config | 3 | Env-var config is good; internal auth and CORS defaults need tightening for prod claims. |
| 5. Testing | 4 | Many backend tests + frontend Vitest tests exist; no single coverage summary captured here. |
| 6. Observability & Ops | 3 | Actuator + metrics endpoints exist; healthcheck tooling might be fragile. |
| 7. Documentation & DX | 3 | README + Presentation are strong; “how to run/demo” could be more explicit. |

### 3. Architecture Fit

Key flow evidence in code:

- Accounts/auth: apps/accounts-service/src/main/java/com/novareport/accounts_service/auth/AuthController.java
- Subscription checks + internal activation: apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/SubscriptionController.java and InternalSubscriptionController.java
- Payments: apps/payments-xmr-service/.../PaymentController.java + PaymentMonitorService.java; Stripe flow in apps/payments-stripe-service/.../StripeWebhookController.java
- Reports: apps/reporter-service/.../ScheduledReportGenerator.java and controller ReportController.java
- Notifications: apps/notifications-service/.../NotificationController.java + DailyReportEmailJob.java + DailyReportDiscordJob.java

### 4. Clean Code & SOLID (Top 10 findings)

1. apps/accounts-service/.../AuthController.java – clear intent and outcome metrics.
2. apps/accounts-service/.../GlobalExceptionHandler.java – consistent ProblemDetail responses for validation.
3. apps/payments-stripe-service/.../StripeWebhookController.java – robust handling + dedicated tests.
4. apps/reporter-service/.../OneMinAiSummarizerService.java – retries + fallback behavior (good demo resilience).
5. apps/notifications-service/.../AdminNotificationController.java – strong ops endpoints for demo/troubleshooting.
6. apps/notifications-service/.../DailyReportEmailJob.java – avoids duplicate sends using emailSentAt.
7. Inconsistency: internal API key filters differ:
   - equals: apps/accounts-service/.../InternalApiKeyFilter.java, apps/subscriptions-service/.../InternalApiKeyFilter.java
   - constant-time: apps/payments-xmr-service/.../InternalApiKeyFilter.java
8. apps/reporter-service/.../ReportNotificationPublisher.java – blocking call is acceptable for jobs, but note it if asked.
9. Path consistency: accounts mixes /auth and /api/accounts/auth.
10. CORS defaults allow wildcard patterns (CorsConfig + prod compose defaults).

### 5. REST & Spring Review

| Service | Endpoint | Verb | Notes |
|--------|----------|------|------|
| accounts | /auth/register | POST | 201; validation; welcome email call in AuthController. |
| subscriptions | /api/v1/subscriptions/me/has-access | GET | Requires uid claim via filter; returns boolean. |
| payments-xmr | /api/v1/payments/create | POST | Creates payment; monitoring confirms later. |
| payments-stripe | /api/v1/payments-stripe/webhook/stripe | POST | Signature verified; returns 428 if secret missing. |
| reporter | /api/v1/reports/latest | GET | Access enforced by SubscriptionAccessService. |
| notifications | /api/v1/notifications/latest | GET | Latest report snapshot. |

### 6. Security & Config

- High: CORS_ALLOWED_ORIGINS defaults to * in deploy/docker-compose.prod.yml.
- Medium: Internal key enforcement differs and some use non-constant-time equals.
- Medium: Some security configs permit internal paths and rely on the internal filter for enforcement (e.g. notifications service).
- Low/Medium: AI requires ONEMIN_API_KEY unless REPORTER_FAKE_AI is enabled.

### 7. Testing Gaps

- Add a short documented end-to-end smoke checklist for the demo flow.
- Add at least one HTTP-level test that internal API key is enforced for an internal endpoint.
- Add a simple test around the scheduled generator orchestration path (or a “run now” trigger).

### 8. Observability & Ops

- Existing: Actuator health/metrics exposed in application-prod.properties for multiple services.
- Existing: Admin metrics endpoints (e.g. accounts AdminController, subscriptions AdminSubscriptionController, notifications AdminNotificationController, payments AdminPaymentController).
- Risk: Compose healthchecks depend on curl being available in containers.

### 9. Action Checklist

1. Align docs with CI reality: fix README-CODE-QUALITY.md reference to code-quality.yml.
2. Write a one-page demo runbook (env vars + steps) referencing deploy/docker-compose.yml and deploy/docker-compose.prod.yml.
3. Verify/adjust prod healthchecks so they work with your runtime images.
4. Decide a demo mode for AI (REPORTER_FAKE_AI fallback vs real ONEMIN_API_KEY).
5. Decide a demo mode for Monero (PAYMENTS_FAKE_MODE vs real wallet).
6. Confirm INTERNAL_API_KEY is shared consistently across services.
7. Tighten CORS_ALLOWED_ORIGINS for the environment you will present.
8. Do one full rehearsal on the same stack you will demo.
9. Prepare 2–3 “failure mode” notes: AI down, Discord down, mail down.
10. Keep a fresh check-code.ps1 output as an appendix artifact.

---

## Swedish Report

### 1. Sammanfattning (Executive Summary)

- Styrka: Tydlig uppdelning i mikrotjänster och en demo-berättelse som stämmer med docs/Presentation.md.
- Styrka: Lokalt kvalitetsskript finns (check-code.ps1) och senaste körningen visar grönt (code-quality-report.txt).
- Styrka: Produktions-Compose finns med env-vars och healthchecks (deploy/docker-compose.prod.yml).
- Styrka: Felhantering/validering är genomgående bra med ProblemDetail-mönster.
- Styrka: Schemalagd rapportgenerering och daglig distribution finns (reporter + notifications jobs).
- Risk: Dokumentation och CI är inte helt synkade (README-CODE-QUALITY.md pekar på workflow som saknas).
- Risk: Healthchecks använder curl; risk att runtime-bilder saknar curl (Dockerfiles använder eclipse-temurin:21-jre).
- Risk: Intern auth med X-INTERNAL-KEY är olika implementerad mellan tjänster (timing-attack-polish).
- Risk: AI-beroende behöver tydlig demo-plan B (REPORTER_FAKE_AI).
- Rekommendation: Sista 2% = demo-runbook + env-var-checklist + reproducibilitet.

### 2. Poängtabell (0–5)

| Område | Poäng | Kommentar |
|-------|------:|-----------|
| 1. Arkitektur | 4 | Tydliga boundaries, lätt att förklara i demo. |
| 2. Clean Code & SOLID | 4 | Överlag bra; vissa inkonsekvenser mellan services. |
| 3. REST & Spring | 4 | Bra struktur; mindre path-inkonsekvenser. |
| 4. Säkerhet & config | 3 | Env-vars bra; CORS och intern-auth behöver tydlighet. |
| 5. Testning | 4 | Många backend-tester + frontendtester. |
| 6. Ops/observability | 3 | Actuator + admin-metrics finns; healthcheck risk. |
| 7. Docs & DX | 3 | README + presentation stark; demo/run-instruktioner kan bli tydligare. |

### 3. Arkitektur (hur ligger vi till)

Kärnflödet finns i kod:

- Konto/login: apps/accounts-service/.../AuthController.java
- Prenumeration/åtkomst: apps/subscriptions-service/.../SubscriptionController.java + InternalSubscriptionController.java
- Betalning: payments-xmr (PaymentController + PaymentMonitorService) och Stripe-webhook (StripeWebhookController)
- Rapporter: reporter-service ScheduledReportGenerator + ReportController
- Notifiering: notifications NotificationController + DailyReportEmailJob + DailyReportDiscordJob

### 4. Clean Code & SOLID (10 punkter)

1. AuthController – tydlig logik och mätning.
2. GlobalExceptionHandler (accounts) – bra ProblemDetail för validering.
3. StripeWebhookController – robust + testad.
4. OneMinAiSummarizerService – retries + fallback.
5. AdminNotificationController – bra för drift/demo.
6. DailyReportEmailJob – idempotens via emailSentAt.
7. Inkonsekvens i InternalApiKeyFilter mellan services.
8. ReportNotificationPublisher – blockerar i jobb (ok, men motivera).
9. Accounts paths: /auth och /api/accounts/auth.
10. CORS default är permissiv i prod.

### 5. REST & Spring

Exempel-endpoints:

- accounts: /auth/register (POST)
- subscriptions: /api/v1/subscriptions/me/has-access (GET)
- payments-xmr: /api/v1/payments/create (POST)
- payments-stripe: /api/v1/payments-stripe/webhook/stripe (POST)
- reporter: /api/v1/reports/latest (GET)
- notifications: /api/v1/notifications/latest (GET)

### 6. Säkerhet & config

- Hög: CORS_ALLOWED_ORIGINS=* i deploy/docker-compose.prod.yml.
- Medel: internnyckelhantering olika och ibland non-constant-time.
- Låg/Medel: AI kräver nyckel eller fake-läge för demo.

### 7. Testluckor

- Skriv en tydlig “smoke test”-checklista för demo-flödet.
- Lägg minst ett HTTP-test som verifierar internnyckel på en intern endpoint.

### 8. Ops/observability

- Actuator health/metrics exponerade i application-prod.properties.
- Admin/metrics endpoints finns i flera services.
- Risk: healthcheck beroende av curl i containern.

### 9. Åtgärdslista (sista 2%)

1. Synka docs/CI-referenser (README-CODE-QUALITY.md).
2. Skapa en demo-runbook (env vars + klickflöde + felsökning).
3. Verifiera healthchecks i prod (curl-problematik).
4. Bestäm demo-mode för AI och Monero.
5. Säkerställ INTERNAL_API_KEY konsekvent.
6. Tighta CORS för demo/prod.
7. Genomför en full repetition på samma stack som du ska visa.
