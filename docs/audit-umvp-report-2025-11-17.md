# Nova Report – UMVP Architecture & Code Audit Report (2025-11-17)

## English Report

### 1. Executive Summary

- Clear microservice boundaries match the intended architecture; the Monero payments flow is now end-to-end and realistic.
- Service responsibilities are mostly well separated (accounts, subscriptions, payments, reports, notifications, frontend), with limited cross-coupling.
- Code style is generally clean with good use of records/DTOs and builders, but some duplication exists in security and JWT handling across services.
- REST endpoints use sensible paths and verbs; status codes are mostly appropriate, though there is no unified error model or Problem Details usage.
- Security is acceptable for UMVP: JWT auth, stateless sessions, internal API keys, and environment-based configuration, but CORS and internal endpoints are somewhat permissive.
- Configuration largely follows 12-factor principles, but some dev defaults and comments risk being copied into production without hardening.
- Observability is decent for a demo (Actuator health, structured logging, Docker health checks) but lacks metrics, tracing, and centralized log correlation.
- Testing is minimal; there are effectively no automated unit or integration tests in the services or frontend yet.
- Frontend provides a coherent user journey with Monero payment QR support, but still has some placeholder README content and limited error surface handling.
- Overall the UMVP is well structured for a junior-level project; the main gaps are tests, consistent error handling, and tightening of security defaults.

### 2. Scores Table (0–5)

| Area | Score | Comment |
|------|-------|---------|
| Architecture Fit | 4 | Services and flow match the intended design; Monero integration follows the envisioned payments role. |
| Clean Code & SOLID | 3 | Generally clean, but security/JWT code is duplicated and some classes mix concerns. |
| REST & Spring Conventions | 3 | Paths/verbs are good; validation exists, but no shared error handling or Problem Details. |
| Security & Config | 3 | JWT, internal API keys, and env vars are solid for UMVP; CORS and internal endpoints could be stricter. |
| Testing | 1 | No automated tests found; risk of regressions is high. |
| Observability & Ops | 3 | Actuator health, logging, Docker and Compose health checks present; metrics and tracing missing. |
| Documentation & DX | 3 | Root README is good; service-level docs and frontend README are mostly template-level. |

### 3. Architecture Fit

- The overall flow `frontend → accounts-service → subscriptions-service → payments-xmr-service → reporter-service → notifications-service → user` is implemented and coherent.
- `accounts-service` exposes `/auth` endpoints and issues JWTs used by other services via `JwtService`.
- `subscriptions-service` manages access via `/api/v1/subscriptions/me/**` and internal `/api/v1/internal/subscriptions/**` endpoints, guarded by an internal API key filter.
- `payments-xmr-service` now performs real Monero integration (`MoneroWalletClient`, `PaymentService`, `PaymentMonitorService`) and activates subscriptions via internal calls to `subscriptions-service`.
- `reporter-service` uses `SubscriptionAccessService` to gate report access based on the subscriptions API, and exposes `/api/v1/reports/**`.
- `notifications-service` receives internal notifications from reporter via `/api/v1/internal/notifications/report-ready` and serves `/api/v1/notifications/latest` to the frontend.
- `frontend` orchestrates the user flow (login, subscribe, pay with Monero, view report) and talks to the backend via reverse-proxied APIs.
- Docker Compose (`deploy/docker-compose.yml`) wires all services, Monero wallet RPC, and frontend together using a shared `novareport` network.
- Boundaries are mostly respected: there is no direct DB sharing between services; communication happens over HTTP.

### 4. Clean Code & SOLID – Top 10 Findings

1. `accounts-service/AuthController` – Good separation of registration and login endpoints, with helper methods for user and settings creation; clear responsibility but could be split if logic grows.
2. `subscriptions-service/SubscriptionController` – Uses helper `resolveUserId` to extract the UID claim; this is clear but duplicates similar logic found elsewhere.
3. `subscriptions-service/JwtService` and similar services in other modules – JWT creation/parsing logic is repeated across services instead of being shared or abstracted, violating DRY.
4. `payments-xmr-service/MoneroWalletClient` – Encapsulates RPC details and now includes proper request/response logging; however, using low-level `HttpURLConnection` instead of `RestTemplate` or WebClient adds boilerplate.
5. `payments-xmr-service/PaymentService` – Clean transaction boundary and use of builder pattern for `Payment`; combining fake-mode and real Monero logic in one class slightly reduces SRP clarity.
6. `payments-xmr-service/PaymentMonitorService` – Simple scheduled polling with good logging, but error handling just logs and continues; no backoff or metrics.
7. `reporter-service/ReportController` – Validates pagination parameters and date ranges explicitly, showing good attention to input validation.
8. `notifications-service/NotificationController` – Clear separation between internal write endpoint and public read endpoint; uses DTO mapping instead of exposing entities.
9. `frontend/src/App.tsx` – Centralized state management in a single large component makes it harder to reason about; extracting views and hooks would improve readability and reusability.
10. Cross-cutting concerns (security, error handling, logging) are implemented per-service, leading to repetition and risk of divergence.

### 5. REST & Spring Review

**Selected endpoints overview**

| Service | Endpoint | Verb | Status Codes | Problem Details |
|---------|----------|------|--------------|-----------------|
| accounts-service | `/auth/register` | POST | 201, 400, 409 | No (throws exceptions) |
| accounts-service | `/auth/login` | POST | 200, 400, 401 | No |
| subscriptions-service | `/api/v1/subscriptions/me/has-access` | GET | 200 | No |
| subscriptions-service | `/api/v1/subscriptions/me` | GET | 200, 404 | No |
| subscriptions-service | `/api/v1/internal/subscriptions/activate` | POST | 200 | No |
| payments-xmr-service | `/api/v1/payments/create` | POST | 201, 400, 401 | No |
| payments-xmr-service | `/api/v1/payments/{id}/status` | GET | 200, 404 | No |
| payments-xmr-service | `/api/v1/internal/payments/{id}/confirm` | POST | 202, 404, 409 | No |
| reporter-service | `/api/v1/reports/latest` | GET | 200, 404, 401 | No |
| reporter-service | `/api/v1/reports` | GET | 200, 400, 401 | No |
| notifications-service | `/api/v1/internal/notifications/report-ready` | POST | 202 | No |
| notifications-service | `/api/v1/notifications/latest` | GET | 200, 404 | No |

Findings:
- Controllers generally use appropriate HTTP verbs and status codes (e.g., 201 for register, 202 for async notification acceptance).
- Validation annotations are used on request DTOs in several services (`@Valid`, `@NotNull`, etc.).
- There is no shared `@ControllerAdvice` or Problem Details implementation; errors are thrown as exceptions and mapped by Spring’s default mechanisms.
- Internal vs external endpoints are clearly separated via path prefixes and/or internal API key filters.

### 6. Security & Config

Findings (with indicative risk levels):

- Use of JWT with symmetric keys (`jwt.secret` via env vars) and issuer enforcement in each service – **Risk: Low** (good pattern; ensure strong secrets in production).
- Internal service-to-service calls protected by `X-INTERNAL-KEY` header (`internal.api-key`) – **Risk: Medium** (simple but effective; ensure key is never exposed to browsers and rotated in production).
- CORS configurations in services are relatively permissive (e.g., wildcard patterns allowed) – **Risk: Medium** (acceptable for UMVP; should be tightened to known origins in production).
- Dev Docker Compose uses hard-coded JWT secrets and internal keys – **Risk: Low/Medium** (OK for local dev; must not be copied to production without changes).
- `payments-xmr-service` exposes Monero RPC URL only internally (no host port mapping for `monero-wallet-rpc`) – **Risk: Low** (good isolation).
- Lack of explicit rate limiting or brute-force protection on login (`/auth/login`) – **Risk: Medium** (important if exposed publicly).
- No centralized input sanitization or output encoding beyond framework defaults – **Risk: Medium** (typical for UMVP, but relevant vs OWASP basics).

### 7. Testing Gaps

Suggested (missing) tests:

- `AccountsAuthControllerTests.register_shouldCreateUser_andReturnJwt()` – Verify happy-path registration and token issuance.
- `AccountsAuthControllerTests.register_shouldRejectDuplicateEmail()` – Ensure proper handling of existing email.
- `AccountsAuthControllerTests.login_shouldRejectInvalidCredentials()` – Validate authentication failure paths.
- `SubscriptionsServiceTests.hasAccess_shouldReflectActiveSubscription()` – Unit tests for subscription access logic.
- `PaymentServiceTests.createPayment_shouldCreateMoneroSubaddress_andPersistPayment()` – Exercise integration with `MoneroWalletClient` (mocked).
- `PaymentMonitorServiceTests.shouldConfirmPayment_whenConfirmedBalanceMeetsAmount()` – Verify polling logic and min-confirmations behaviour.
- `ReporterServiceTests.latest_shouldRequireValidSubscription()` – Ensure access control is enforced.
- `NotificationControllerTests.latest_shouldReturnNotFoundWhenNoReport()` – Verify 404 path.
- Frontend tests: e.g. `SubscribeView.test.tsx` for address display, QR rendering, and polling state transitions.

### 8. Observability & Ops

Existing:

- Spring Boot Actuator health endpoints enabled and wired into Docker health checks for several services.
- Logs include useful contextual information (user IDs, payment IDs, RPC request/response summaries for Monero).
- Docker Compose and prod Compose define clear service dependencies and health checks.

Missing or limited:

- No metrics endpoints or dashboards (Prometheus / Grafana) for monitoring key flows (logins, payments, report generation).
- No distributed tracing or correlation IDs across services.
- No structured logging standard (e.g., JSON logs with consistent fields) across services.
- No documented operational runbooks (e.g., how to rotate keys, handle Monero node outages).

### 9. Action Checklist (Prioritized)

1. Introduce a shared error handling strategy using `@ControllerAdvice` and RFC 7807 Problem Details for public APIs.
2. Add basic unit tests for critical flows in `accounts-service`, `subscriptions-service`, `payments-xmr-service`, and `reporter-service`.
3. Tighten CORS configurations to known frontend origins for production deployments.
4. Centralize JWT handling patterns or document them clearly to avoid divergence between services.
5. Add rate limiting/brute-force protection or account lockout on `/auth/login`.
6. Implement minimal metrics (e.g., request counts, payment confirmations, report generations) and expose via Actuator.
7. Improve frontend structure by splitting `App.tsx` into smaller view components and adding a few key UI tests.
8. Document production configuration requirements (secrets, internal keys, Monero daemon endpoints) in `docs/`.
9. Add support for correlation IDs in logs to trace a request across services.
10. Define a simple incident playbook for payment failures or Monero RPC downtime.
11. Review and tighten internal API key usage to ensure keys are never sent to browsers.
12. Add integration tests that spin up a subset of services (e.g., accounts + subscriptions + reporter) using Testcontainers or similar.

---

## Svensk Rapport

### 1. Sammanfattning

- Arkitekturen följer i stort den tänkta mikroservice-modellen och betalflödet med Monero fungerar nu från UI till backend och tillbaka.
- Tjänsterna har tydliga ansvarsområden (konton, prenumerationer, betalningar, rapporter, notifieringar, frontend) och kopplingen mellan dem är rimlig.
- Kodstilen är överlag ren med bra användning av DTOs och builder-mönster, men säkerhets- och JWT-kod upprepas i flera tjänster.
- REST-endpoints använder vettiga URL:er och HTTP-verb, men det saknas ett gemensamt felformat (t.ex. Problem Details).
- Säkerheten är okej för en UMVP: JWT, stateless sessions, interna API-nycklar och konfiguration via env-variabler, men CORS och interna endpoints kan skärpas.
- Observability är tillräcklig för demo (Actuator health, loggar, Docker health checks), men saknar metrics och tracing.
- Det finns i princip inga automatiska tester, vilket gör projektet känsligt för regressionsfel.
- Frontenden ger ett bra flöde för användaren, inklusive QR-kod för Monero, men README och struktur är fortfarande delvis mallbaserade.
- Dokumentation på rot-nivå är bra, men det saknas mer detaljerad tjänst-specifik dokumentation och driftinstruktioner.
- Helhetsintrycket är starkt för ett junior-projekt; största förbättringsområdena är tester, felhantering och skarpare säkerhetsinställningar.

### 2. Poängtabell (0–5)

| Område | Poäng | Kommentar |
|--------|-------|-----------|
| Arkitektur | 4 | Tjänster och flöde matchar design; Monero-betalningar är väl integrerade. |
| Clean Code & SOLID | 3 | Ren kod generellt, men viss duplicering och blandade ansvar. |
| REST & Spring | 3 | Bra endpoints och statuskoder, men ingen gemensam felmodell. |
| Säkerhet & konfig | 3 | Bra grund med JWT och interna nycklar; CORS och login-skydd kan stärkas. |
| Testning | 1 | Nästan inga tester; hög risk för oväntade buggar. |
| Observability & drift | 3 | Health checks och loggar finns; metrics/tracing saknas. |
| Dokumentation & DX | 3 | Bra övergripande README, mindre bra på tjänstnivå. |

### 3. Arkitektur

- Flödet från frontend via konton → prenumerationer → betalningar → rapporter → notifieringar är tydligt och konsekvent.
- Varje Spring Boot-tjänst har sitt eget ansvar och egen databas; kommunikation sker via HTTP-API:er, inte delad DB.
- Monero-integrationen ligger isolerad i `payments-xmr-service` och pratar med `subscriptions-service` via interna endpoints.
- Docker Compose-definitionerna kopplar ihop tjänsterna och Monero-wallet-rpc på ett överskådligt sätt.

### 4. Clean Code & SOLID – 10 observationer

1. `AuthController` i accounts-service är tydlig men relativt stor; kan delas upp om logiken växer.
2. `SubscriptionController` har bra helpers men duplicerar UID-hantering som finns på fler ställen.
3. `JwtService` upprepas i flera tjänster med liknande kod – bryter mot DRY.
4. `MoneroWalletClient` kapslar in RPC-anrop väl, men låg-nivå HTTP gör koden mer detaljerad än nödvändigt.
5. `PaymentService` hanterar både fake-mode och riktig Monero, vilket gör ansvar något blandat.
6. `PaymentMonitorService` är enkel och lätt att läsa, men felhantering och metrics är minimala.
7. `ReportController` visar bra inputvalidering för paginering och datumintervall.
8. `NotificationController` skiljer internal/public-endpoints tydligt och använder DTOs.
9. `App.tsx` i frontenden är stor och innehåller mycket state; uppdelning i mindre komponenter och hooks skulle öka läsbarheten.
10. Cross-cutting-kod (säkerhet, felhantering, logging) hanteras per tjänst i stället för via gemensamma moduler eller tydliga riktlinjer.

### 5. REST & Spring

- De viktigaste endpoints följer etablerade mönster: `/auth/**` för inloggning/registrering, `/api/v1/subscriptions/me/**` för prenumerationer, `/api/v1/payments/**` för betalningar, `/api/v1/reports/**` för rapporter och `/api/v1/notifications/**` för notifieringar.
- Validering med `@Valid` och bean validation används på flera request-objekt, vilket är bra.
- Det saknas gemensam `@ControllerAdvice` och Problem Details; fel blir därmed mindre förutsägbara för klienten.
- Interna endpoints har egna path-prefix och skyddas med interna API-nycklar.

### 6. Säkerhet & konfiguration

- JWT med hemlighet och issuer per tjänst ger en bra bas; hemligheter måste dock hanteras säkert i drift.
- Intern API-nyckel (`X-INTERNAL-KEY`) skyddar interna endpoints, men kräver noggrann hantering så att nyckeln inte läcker till klienter.
- CORS-konfigurationer är relativt öppna och bör stramas åt mot kända origin i produktion.
- Login-endpoints saknar separat skydd mot brute-force, vilket är acceptabelt i UMVP men bör adresseras vid riktig drift.

### 7. Testluckor

- Inga tydliga enhetstester eller integrationstester hittades för backend-tjänsterna.
- Frontend saknar tester för kritiska flöden som login, prenumeration och betalning.
- Det finns inga tester som täcker hela flödet från betalning till aktiverad prenumeration och rapportåtkomst.

### 8. Observability & drift

- Actuator health används och är kopplad till Docker health checks, vilket är bra.
- Loggar innehåller nyttig kontext (t.ex. paymentId och Monero-RPC-svar), men saknar standardiserad struktur och korrelations-ID.
- Metrics, tracing och larm/logg-aggregering saknas eller är inte dokumenterade.

### 9. Åtgärdslista

1. ✅ Inför central felhantering och Problem Details för externa API:er (klar 2025-11-18).
2. Lägg till grundläggande enhetstester för auth, prenumerationer, betalningar och rapporter.
3. Strama åt CORS för produktionsmiljö och dokumentera rekommenderade värden.
4. Standardisera JWT-hantering (antingen genom gemensam modul eller dokumenterad konvention).
5. Lägg till skydd mot brute-force på login, t.ex. rate limiting eller kontolåsning.
6. Inför enkla metrics och exponera dem via Actuator för att kunna följa nyckelflöden.
7. Dela upp `App.tsx` i mindre komponenter och introducera några centrala UI-tester.
8. Dokumentera produktionskonfiguration (hemligheter, interna nycklar, Monero-noder) i `docs/`.
9. ✅ Lägg till korrelations-ID i loggar så att en begäran kan följas mellan tjänster (klar 2025-11-18).
10. Skapa en kort driftmanual för hur betalningsproblem och Monero-RPC-nedtid hanteras.
