# Nova Report – UMVP Architecture & Code Audit Report
Date: November 14, 2025  
Auditor: Cascade  
Project Version: UMVP (Ultra-Minimum Viable Product)

---

# ENGLISH VERSION

## 1. Executive Summary

- Clear microservice boundaries across accounts-service, subscriptions-service, payments-xmr-service, reporter-service, notifications-service and frontend apps.
- Actual runtime flow closely matches the intended architecture from audit-umvp.md and README (login → subscription → payment → report → notification).
- Error handling is now consistently centralized with ProblemDetail or structured error objects in all backend services.
- Production configuration uses PostgreSQL with Flyway for all services, following 12-Factor principles and avoiding H2 in prod.
- Observability and ops improved: docker-compose.prod.yml defines health checks for all services using /actuator/health, and actuator health exposure is configured.
- Reporter-service has grown in scope with AI summarisation, startup hooks and scheduling; powerful but increases complexity beyond the original “without AI” UMVP.
- Frontend App.tsx remains a large god component that mixes UI, state, and all HTTP calls in one file.
- Automated tests are still effectively limited to generated ApplicationTests; there are no focused unit, controller or integration tests for core flows.
- Security is reasonable for a UMVP (env-based secrets, non-root Docker users) but lacks rate limiting, JWT secret strength checks and per-service internal API keys.
- Documentation covers architecture well but lacks detailed setup, environment examples and consolidated API documentation for all services.

## 2. Scores Table (0–5)

| Area                     | Score | Comment |
|--------------------------|-------|---------|
| 1. Architecture Fit      | 4     | Clear service boundaries and expected flow; still tightly coupled via internal HTTP calls and shared internal API key. |
| 2. Clean Code & SOLID    | 3     | Service code is mostly clean and cohesive; frontend App.tsx is too large and some controllers use generic exceptions. |
| 3. REST & Spring         | 4     | Good use of verbs, status codes, DTOs and global handlers; reporter-service uses a custom error DTO instead of ProblemDetail. |
| 4. Security & Config     | 3     | Env-based secrets and Postgres/Flyway are strong; shared internal key and no rate limiting remain gaps. |
| 5. Testing               | 1     | Only basic context-load tests exist; no meaningful automated tests for core domain flows. |
| 6. Observability & Ops   | 4     | Actuator health, Docker health checks and good logging; metrics, tracing and central logging are still missing. |
| 7. Documentation & DX    | 2     | High-level docs exist; setup, env examples and API docs are incomplete. |

Overall: 3.0/5 – solid UMVP architecture and production config, but testing and security depth are not yet at junior-ready level.

## 3. Architecture Fit

Intended flow from audit-umvp.md and README: frontend → accounts-service → subscriptions-service → payments-xmr-service (mocked) → reporter-service → notifications-service → user.

Evidence in code and docker-compose.prod.yml:

- Accounts-service (apps/accounts-service)
  - AuthController exposes registration and login endpoints under /auth and /api/accounts/auth.
  - UserController exposes /api/accounts/me and /api/accounts/me/settings for profile and settings.
- Subscriptions-service (apps/subscriptions-service)
  - SubscriptionController exposes /api/v1/subscriptions/me/has-access and /api/v1/subscriptions/me.
  - InternalSubscriptionController exposes /api/v1/internal/subscriptions/activate and /cancel.
- Payments-xmr-service (apps/payments-xmr-service)
  - PaymentController exposes /api/v1/payments/create and /api/v1/payments/{paymentId}/status.
  - InternalPaymentController exposes /api/v1/internal/payments/{paymentId}/confirm.
- Reporter-service (apps/reporter-service)
  - ReportController exposes /api/v1/reports/latest and /api/v1/reports.
  - ReporterCoordinator coordinates RssIngestService, DailyReportService and ReportNotificationPublisher.
  - ScheduledReportGenerator runs every 4 hours; StartupReportGenerator can ingest and build on startup.
- Notifications-service (apps/notifications-service)
  - NotificationController exposes POST /api/v1/internal/notifications/report-ready and GET /api/v1/notifications/latest.
- Frontend (apps/frontend)
  - App.tsx calls /api/accounts/auth, /api/accounts/me, /api/subscriptions/me, /api/payments, and /api/notifications/latest and presents the end-user flow.

Deviations:

- Reporter-service now uses AI summarisation (OneMinAiSummarizerService) and external 1min.ai API; this goes beyond the original “without AI” UMVP scope but is implemented behind a configuration flag and fallback.
- Services use direct HTTP calls and a shared INTERNAL_API_KEY for internal communication instead of a gateway or message bus; acceptable for UMVP but less flexible in the long run.

Conclusion: The implemented architecture matches the intended microservice design and flow very well, with deliberate compromises around coupling and AI usage.

## 4. Clean Code & SOLID – Top 10 Findings

1. PaymentService removes magic numbers with clear constants
   - File: apps/payments-xmr-service/src/main/java/com/novareport/payments_xmr_service/service/PaymentService.java
   - Impact: Easier to reason about expiry and plan durations; better readability and maintainability.

2. GlobalExceptionHandler with ProblemDetail in most services
   - Files: GlobalExceptionHandler in accounts-service, subscriptions-service, payments-xmr-service, notifications-service
   - Impact: Central, consistent error handling aligned with REST best practices.

3. Reporter-service uses a custom ApiError instead of ProblemDetail
   - File: apps/reporter-service/src/main/java/com/novareport/reporter_service/exception/GlobalExceptionHandler.java
   - Impact: Still centralized handling but slightly inconsistent error format across services.

4. DailyReportService is cohesive but could later be split further
   - File: apps/reporter-service/src/main/java/com/novareport/reporter_service/service/DailyReportService.java
   - Impact: Currently readable and focused; future growth may justify separating summary creation, data selection and persistence.

5. OneMinAiSummarizerService encapsulates retries and clean fallback
   - File: apps/reporter-service/src/main/java/com/novareport/reporter_service/service/OneMinAiSummarizerService.java
   - Impact: External API failures are handled robustly with retries, logging and user-friendly fallback summary text.

6. RssIngestService has clear helper methods but is dense
   - File: apps/reporter-service/src/main/java/com/novareport/reporter_service/service/RssIngestService.java
   - Impact: Good decomposition into methods; complexity could be reduced by extracting feed fetching and entity mapping into smaller collaborators.

7. App.tsx is a large god component
   - File: apps/frontend/src/App.tsx
   - Impact: Violates single responsibility, makes the UI hard to test and evolve; should be split into feature components.

8. UserController uses NoSuchElementException for domain errors
   - File: apps/accounts-service/src/main/java/com/novareport/accounts_service/user/UserController.java
   - Impact: Generic exceptions make error handling less explicit; domain-specific exceptions handled by GlobalExceptionHandler would be clearer.

9. Manual uid resolution in SubscriptionController
   - File: apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/SubscriptionController.java
   - Impact: Works for now but repeats a pattern; a custom argument resolver or security helper would reduce duplication.

10. Clear separation of public and internal controllers
    - Files: controllers under apps/subscriptions-service, apps/payments-xmr-service, apps/notifications-service
    - Impact: Public vs internal endpoints are clearly separated by path and controller, which aids security and documentation.

## 5. REST & Spring Review

Selected key endpoints:

| Service              | Path                                           | Verb  | Status codes (main)  | Structured error |
|----------------------|------------------------------------------------|-------|----------------------|------------------|
| accounts-service     | /api/accounts/auth/register                    | POST  | 201, 400             | Yes (ProblemDetail) |
| accounts-service     | /api/accounts/auth/login                       | POST  | 200, 400, 401        | Yes (AccountsException → ProblemDetail) |
| accounts-service     | /api/accounts/me                               | GET   | 200, 404             | Partially (generic NoSuchElementException) |
| subscriptions-service| /api/v1/subscriptions/me/has-access            | GET   | 200, 400             | Yes (ResponseStatusException handled) |
| subscriptions-service| /api/v1/subscriptions/me                       | GET   | 200, 404             | Yes |
| payments-xmr-service | /api/v1/payments/create                        | POST  | 201, 400             | Yes |
| payments-xmr-service | /api/v1/payments/{paymentId}/status            | GET   | 200, 404             | Yes |
| reporter-service     | /api/v1/reports/latest                         | GET   | 200, 404             | Yes (ApiError) |
| reporter-service     | /api/v1/reports                                | GET   | 200, 400             | Yes (ApiError) |
| notifications-service| /api/v1/internal/notifications/report-ready    | POST  | 202, 400             | Yes |
| notifications-service| /api/v1/notifications/latest                   | GET   | 200, 404             | Yes |

Controllers use appropriate Spring annotations, DTOs, and validation. The main improvement would be to standardise error response format (ProblemDetail) across all services and remove generic exceptions in controllers.

## 6. Security & Config

Findings with risk level:

- JWT configuration via env vars (Low–Medium)
  - JWT issuer and secret are configured through environment variables in each service. Good practice, but no explicit startup validation of secret strength.

- Shared INTERNAL_API_KEY across services (Medium)
  - docker-compose.prod.yml and application properties show a single internal.api-key used in multiple places. If leaked, it grants broad access to internal endpoints.

- Postgres + Flyway for production (Low, positive)
  - application-prod.properties in all services configure PostgreSQL with Flyway migrations and validate DDL, which is appropriate for production.

- No rate limiting or account lockout (Medium)
  - Auth endpoints and payment endpoints have no visible rate limiting or lockout strategy, leaving room for brute-force or abuse.

- AI API key via env (Low–Medium)
  - onemin.api-key is provided through ONEMIN_API_KEY and not hard-coded. Missing configuration leads to fallback behaviour, which is acceptable for UMVP.

## 7. Testing Gaps

Current automated tests are limited to basic ApplicationTests that start the Spring context. Important missing tests include:

- accounts-service: AuthController tests for register/login, UserController tests for profile/settings, JwtService tests.
- subscriptions-service: SubscriptionService tests for access logic; controller tests for me/has-access and me endpoints.
- payments-xmr-service: PaymentService tests for create/confirm; PaymentController tests; tests for events leading to subscription activation.
- reporter-service: RssIngestService tests for parsing and dedup; DailyReportService tests for summary generation; OneMinAiSummarizerService tests for retry/fallback; ReportController tests.
- notifications-service: NotificationReportService and NotificationController tests.
- frontend: Jest + React Testing Library tests for login, subscription, payment polling and report rendering.

## 8. Observability & Ops

Existing elements:

- Actuator health endpoints exposed and used in docker-compose.prod.yml healthchecks for all backend services.
- SLF4J logging throughout, with LogSanitizer in payments-xmr-service to avoid sensitive values in logs.
- Reporter-service has explicit logging around RSS ingest, report building, and notification publishing.
- OneMinAiSummarizerService and RssIngestService implement retries/fallbacks with clear log messages.

Missing or limited:

- No explicit metrics (e.g. Prometheus) for request rates or domain events.
- No tracing or correlation IDs across services.
- No central log aggregation configuration in the repo.
- Health endpoints do not distinguish readiness vs liveness.
- No documented monitoring/alerting or resource limits in docker-compose.prod.yml.

## 9. Action Checklist

Priority 1 – Critical

1. Add unit tests for PaymentService, DailyReportService and SubscriptionService.
2. Add controller/integration tests for AuthController, SubscriptionController, PaymentController and ReportController.
3. Introduce startup validation of JWT_SECRET (presence and minimum strength) in each service.
4. Implement basic rate limiting for authentication and payment endpoints.
5. Refine UserController to use domain-specific exceptions mapped via GlobalExceptionHandler.

Priority 2 – High

6. Refactor apps/frontend/src/App.tsx into smaller feature components.
7. Standardise error response format across services by migrating reporter-service to ProblemDetail.
8. Replace the single INTERNAL_API_KEY with per-service keys or plan for mutual TLS.
9. Extend README (or a dedicated docs/setup.md) with environment variable examples and local/prod setup instructions.

Priority 3 – Medium

10. Expose basic metrics and prepare a simple dashboard for key domain events (reports per day, payments per day).
11. Introduce correlation IDs in HTTP headers and log statements.
12. Split healthchecks into readiness and liveness where meaningful.
13. Validate critical env vars (DB_HOST, DB_USER, INTERNAL_API_KEY, ONEMIN_API_KEY when AI is enabled) at startup with clear failures.
14. Add a short operations guide describing deployment, monitoring and troubleshooting procedures.

---

# SVENSK VERSION

## 1. Sammanfattning

- Tydliga mikrotjänstgränser mellan accounts-service, subscriptions-service, payments-xmr-service, reporter-service, notifications-service och frontend.
- Det faktiska flödet matchar den avsedda arkitekturen (inloggning → prenumeration → betalning → rapport → notifiering).
- Felhantering är nu centraliserad med ProblemDetail eller strukturerade felobjekt i alla backend-tjänster.
- Produktionskonfigurationen använder PostgreSQL och Flyway i alla tjänster, enligt 12-Factor och utan H2 i production-profiler.
- Observerbarhet och drift har förbättrats: docker-compose.prod.yml har healthchecks mot /actuator/health för alla tjänster.
- Reporter-service har vuxit med AI-summering, startkrok och scheduler; funktionellt bra men mer komplext än ursprungs-UMVP utan AI.
- Frontend-filen App.tsx är fortfarande en stor god-komponent med både UI, state och alla API-anrop samlade.
- Automatiska tester är i princip begränsade till ApplicationTests som bara startar Spring-konteksten.
- Säkerheten är acceptabel för UMVP men saknar rate limiting, kontoskydd mot brute force och kontroll av JWT-hemlighetens styrka.
- Dokumentation och DX är godkända på hög nivå men saknar detaljerad setup- och API-dokumentation.

## 2. Poängtabell (0–5)

| Område                  | Poäng | Kommentar |
|-------------------------|-------|-----------|
| 1. Arkitekturpassning   | 4     | Tydliga tjänstegränser och rätt flöde; tight koppling via interna anrop och delad intern nyckel. |
| 2. Clean Code & SOLID   | 3     | Bra struktur i services; App.tsx är för stor, vissa controllers använder generiska undantag. |
| 3. REST & Spring        | 4     | Rimlig användning av HTTP-verb, statuskoder, DTO:er och global felhantering; reporter-service har eget felformat. |
| 4. Säkerhet & Konfig    | 3     | Miljövariabler och Postgres/Flyway är bra; delad INTERNAL_API_KEY och ingen rate limiting är kvarvarande risker. |
| 5. Testning             | 1     | Endast grundläggande ApplicationTests; saknar riktiga tester för kärnflöden. |
| 6. Observerbarhet & Ops | 4     | Actuator health, Docker healthchecks och omfattande loggning; metrics, tracing och central logg saknas. |
| 7. Dokumentation & DX   | 2     | Bra översikt i README, men setup och API-detaljer är begränsade. |

## 3. Arkitekturpassning

- Avsedd arkitektur: frontend → accounts-service → subscriptions-service → payments-xmr-service → reporter-service → notifications-service → användare.
- Implementerad:
  - accounts-service sköter registrering, inloggning, profil och inställningar.
  - subscriptions-service sköter prenumerationsstatus för aktuell användare och interna aktiverings-/avslutsendpoints.
  - payments-xmr-service skapar och följer upp betalningar, samt publicerar events för prenumerationsaktivering.
  - reporter-service läser RSS, bygger rapporter och skickar notifieringar.
  - notifications-service lagrar och exponerar senaste rapport-notifiering.
  - frontend knyter ihop hela flödet via proxade /api-anrop.
- Avvikelser: reporter-service använder nu AI-summering och externa API:er; intern kommunikation sker via HTTP-anrop och en gemensam INTERNAL_API_KEY.

## 4. Clean Code & SOLID – Topp 10 fynd

1. PaymentService använder namngivna konstanter istället för magiska tal – förbättrar läsbarhet.  
2. GlobalExceptionHandler med ProblemDetail i de flesta tjänster – ger konsekventa API-fel.  
3. Reporter-service använder eget ApiError-format – fungerar men avviker från övriga tjänster.  
4. DailyReportService är relativt fokuserad – på sikt kan ansvar delas upp ytterligare.  
5. OneMinAiSummarizerService kapslar in retry och fallback – robust hantering av extern AI-tjänst.  
6. RssIngestService har flera hjälpfunktioner men är kompakt – fler små klasser skulle förenkla testning.  
7. App.tsx är en god-komponent – bör delas upp i mindre komponenter per delområde.  
8. UserController använder NoSuchElementException för domänfel – bör bytas mot domänspecifika undantag.  
9. SubscriptionController gör manuell uid-hantering – kan ersättas av gemensam argument-resolver.  
10. Tydlig separation mellan publika och interna controllers – underlättar säkerhet och förståelse.

## 5. REST & Spring-granskning

- Endpoints är generellt rimligt namngivna och använder rätt HTTP-verb (GET/POST) och statuskoder (200/201/202/400/404/409).
- DTO:er används för in- och utdata; validering sker med @Valid där det är relevant.
- GlobalExceptionHandler i samtliga tjänster utom reporter-service använder ProblemDetail; reporter-service har ApiError men följer liknande mönster.
- Förbättringar: standardisera felformat, ersätt generiska undantag i controllers med domänspecifika som mappas centralt.

## 6. Säkerhet & Konfiguration

- JWT via miljövariabler: bra att hemligheter inte hårdkodas, men ingen kontroll av styrka eller minsta längd.
- Delad INTERNAL_API_KEY: förenklar UMVP men ökar risk om nyckeln läcker.
- Postgres och Flyway i produktion: starkt plus; H2 är reserverat för dev-profiler.
- Ingen rate limiting eller kontolåsning på auth- och betalningsendpoints: medel risk.
- AI-nyckel (ONEMIN_API_KEY) via env: ingen hårdkodning, fallback hanterar avsaknad på ett mjukt sätt.

## 7. Testluckor

- Endast context-load tester (ApplicationTests) finns.
- Saknade tester inkluderar:
  - Unit-tester för PaymentService, DailyReportService, SubscriptionService m.fl.
  - Controller-/integrationstester för AuthController, SubscriptionController, PaymentController, ReportController.
  - Tester för RssIngestService, OneMinAiSummarizerService och NotificationReportService.
  - Frontend-tester med Jest/React Testing Library för login, prenumeration, betalning och rapportvisning.

## 8. Observerbarhet & Ops

- Finns: Actuator health, Docker healthchecks, omfattande loggning och retry/fallback för externa beroenden.
- Saknas: metrics, tracing, central logghantering, readiness/liveness-uppdelning och dokumenterad övervakningsstrategi.

## 9. Åtgärdschecklista

Prioritet 1 – Kritiskt

1. Inför unit-tester för kärnservicelagret (PaymentService, DailyReportService, SubscriptionService).  
2. Lägg till controller-/integrationstester för AuthController, SubscriptionController, PaymentController och ReportController.  
3. Lägg till uppstartsvalidering för JWT_SECRET (närvaro och minsta styrka).  
4. Implementera enkel rate limiting för auth- och betalningsendpoints.  
5. Byt ut generiska undantag i UserController mot domänspecifika undantag som hanteras i GlobalExceptionHandler.

Prioritet 2 – Hög

6. Refaktorera apps/frontend/src/App.tsx till mindre komponenter.  
7. Standardisera felformat genom att låta reporter-service använda ProblemDetail.  
8. Minska beroendet på en enda INTERNAL_API_KEY; planera för separata nycklar eller mutual TLS.  
9. Utöka README eller skapa docs/setup.md med miljövariabler och körinstruktioner.

Prioritet 3 – Medel

10. Exponera grundläggande metrics och skapa en enkel dashboard.  
11. Inför korrelations-id i headers och loggar.  
12. Inför readiness- och liveness-healthchecks där det är relevant.  
13. Validera kritiska miljövariabler vid startup med tydliga felmeddelanden.  
14. Dokumentera en kort driftguide för deployment, övervakning och felsökning.
