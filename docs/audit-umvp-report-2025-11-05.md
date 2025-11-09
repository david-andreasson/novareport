# Nova Report UMVP – Code Audit Report
**Date:** 2025-11-05  
**Auditor:** Claude Sonnet 4.5

---

# ENGLISH REPORT

## 1. Executive Summary

- Service flow partially implemented; payments-xmr-service lacks API breaking the chain
- Reporter-service enforces subscription checks but brittle error handling surfaces 502s  
- Clean controller/service separation but duplicated exception handling patterns
- Inconsistent endpoint validation; internal APIs validated but public rely on attributes
- Security configs expose internal endpoints without API key enforcement
- Hard-coded dev secrets in properties and compose file signal poor secret hygiene
- Test coverage effectively zero beyond context loads
- Observability limited to logging; no Actuator metrics or tracing
- Docker Compose wires services but exposes H2 without access controls
- README communicates vision but lacks setup steps and quality expectations

## 2. Scores Table (0–5)

| Area | Score | Comment |
|------|-------|---------|
| Architecture Fit | 2 | Payments service absent; reporter orchestrates limited flow |
| Clean Code & SOLID | 3 | Services readable with DI, yet validation gaps reduce cohesion |
| REST & Spring Conventions | 2 | Basic verb use ok, but inconsistent ProblemDetails |
| Security & Config | 2 | Internal endpoints whitelisted without key enforcement |
| Testing | 1 | Only context smoke tests exist |
| Observability & Ops | 2 | Logging present, but no Actuator metrics beyond defaults |
| Documentation & DX | 3 | README outlines architecture but lacks onboarding |

## 3. Architecture Fit

Reporter-service acts as the central orchestrator, triggering ingestion, report builds, and notifications through coordination of RSS ingest, report persistence, and downstream notification publishing at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/ReporterCoordinator.java. It calls subscriptions-service via SubscriptionAccessService to validate access before returning reports, relying on WebClient calls to /api/v1/subscriptions/me/has-access at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/SubscriptionAccessService.java. Notifications are forwarded using ReportNotificationPublisher, which expects a configured base URL and internal API key at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/ReportNotificationPublisher.java.

However, payments-xmr-service exposes no controllers or domain logic, leaving the subscription activation path unimplemented at apps/payments-xmr-service/src/main/java/com/novareport/payments_xmr_service/PaymentsXmrServiceApplication.java. The expected flow from frontend to accounts, subscriptions, payments, reporter, and notifications therefore breaks at the payments stage. Additionally, subscriptions-service toggles subs.fake-all-active allowing bypass of real subscription checks at apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java, which undermines boundary enforcement. Internal endpoints for ingestion and notifications are exposed without gateway separation at apps/reporter-service/src/main/java/com/novareport/reporter_service/controller/InternalReporterController.java.

## 4. Clean Code & SOLID – Top 10 Findings

1. ReportController cleanly separates pagination logic but throws generic IllegalArgumentException instead of typed errors at apps/reporter-service/src/main/java/com/novareport/reporter_service/controller/ReportController.java lines 63-79.
2. SubscriptionAccessService encapsulates external call but mixes token parsing and network logic at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/SubscriptionAccessService.java lines 33-75.
3. RssIngestService uses Reactor elegantly yet mixes fetch, parse, dedupe, and persistence in one class at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/RssIngestService.java lines 46-167.
4. DailyReportService abstracts summarization via interfaces for fake/AI – good extensibility at apps/reporter-service/src/main/java/com/novareport/reporter_service/service/DailyReportService.java lines 31-105.
5. SubscriptionController relies on request attributes for uid, coupling to filters without validation fallback at apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/SubscriptionController.java lines 46-56.
6. InternalSubscriptionController correctly annotates DTOs with validation at apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/InternalSubscriptionController.java lines 25-37.
7. NotificationsClient concatenates URLs manually, risking malformed URIs at apps/reporter-service/src/main/java/com/novareport/reporter_service/client/NotificationsClient.java lines 23-37.
8. GlobalExceptionHandler in accounts-service returns RFC 7807 ProblemDetail at apps/accounts-service/src/main/java/com/novareport/accounts_service/common/GlobalExceptionHandler.java lines 15-47.
9. Reporter-service exception handler returns custom ApiError type, deviating from ProblemDetails at apps/reporter-service/src/main/java/com/novareport/reporter_service/exception/GlobalExceptionHandler.java lines 12-44.
10. Lack of interfaces around repositories in subscriptions-service makes testing difficult at apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java lines 18-85.

## 5. REST & Spring Review

| Path | Verb | Status | ProblemDetails | Reference |
|------|------|--------|----------------|-----------|
| /auth/register | POST | 201, 400 | Yes | apps/accounts-service AuthController |
| /auth/login | POST | 200, 401 | Yes | apps/accounts-service AuthController |
| /me | GET | 200, 500 | No | apps/accounts-service UserController |
| /me/settings | PUT | 200, 400 | No | apps/accounts-service UserController |
| /api/v1/subscriptions/me/has-access | GET | 200, 400 | No | apps/subscriptions-service SubscriptionController |
| /api/v1/subscriptions/me | GET | 200, 404 | No | apps/subscriptions-service SubscriptionController |
| /api/v1/internal/subscriptions/activate | POST | 200 | Validated | apps/subscriptions-service InternalSubscriptionController |
| /api/v1/reports/latest | GET | 200, 404 | No | apps/reporter-service ReportController |
| /api/v1/reports | GET | 200, 400 | No | apps/reporter-service ReportController |
| /api/v1/internal/reporter/* | POST/GET | 202, 200 | No | apps/reporter-service InternalReporterController |
| /api/v1/notifications/latest | GET | 200, 404 | No | apps/notifications-service NotificationController |

Controller design inconsistencies include lack of @Validated on some controllers and reliance on exceptions for flow control. Error responses vary between RFC 7807 and custom structures.

## 6. Security & Config

- MEDIUM: Reporter-service permits /api/v1/internal/** without verifying X-INTERNAL-KEY at apps/reporter-service/src/main/java/com/novareport/reporter_service/config/SecurityConfig.java
- MEDIUM: Docker Compose ships hard-coded JWT secrets and internal keys at deploy/docker-compose.yml
- HIGH: Reporter-service WebClient trusts downstream URLs without SSRF safeguards at apps/reporter-service/src/main/java/com/novareport/reporter_service/client/SubscriptionsClient.java
- LOW: Accounts-service logs security debug info in dev config at apps/accounts-service/src/main/resources/application-dev.properties
- MEDIUM: Missing CSRF for browser-facing endpoints at apps/accounts-service/src/main/java/com/novareport/accounts_service/config/SecurityConfig.java
- MEDIUM: Subscription service allows fake-all-active flag at apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java
- HIGH: No rate limiting or brute-force protection on /auth/login at apps/accounts-service/src/main/java/com/novareport/accounts_service/auth/AuthController.java

## 7. Testing Gaps

- AuthControllerLogin_SucceedsWithValidCredentials – Verify token issuance and activity logging
- AuthControllerRegister_FailsOnDuplicateEmail – Ensure EmailAlreadyExistsException returns correct ProblemDetail
- SubscriptionServiceHasAccess_RespectsActiveWindow – Cover fakeAllActive flag and repository query behavior
- ReportControllerList_RejectsInvalidPagination – Assert error mapping for negative page/size
- SubscriptionAccessServiceAssertAccess_HandlesDownstreamFailures – Mock WebClient failures
- ReportNotificationPublisherPublish_SkipsWithoutConfig – Confirm logging and skip behavior
- NotificationControllerReportReady_ValidatesPayload – Validate annotation-driven constraints

## 8. Observability & Ops

- Existing: Structured logging in reporter-service ingest and notification flows
- Existing: Docker Compose orchestrates services with shared network
- Missing: Actuator endpoints (metrics, info, readiness) not enabled beyond health
- Missing: Centralized logging/trace correlation IDs across services
- Missing: Health checks for databases or external dependencies
- Missing: Observability for WebClient timeouts beyond simple logs
- Missing: Structured log formats (JSON) for aggregation

## 9. Action Checklist

1. Implement payments-xmr-service API to activate subscriptions
2. Enforce internal API key verification with strict header checks
3. Replace plaintext secrets with environment-only configuration
4. Standardize error responses on RFC 7807 across all services
5. Add request validation and DTOs for all controllers
6. Introduce integration and unit tests covering core logic
7. Harden WebClient interactions with retry/backoff and circuit breakers
8. Enable Spring Actuator with health, metrics, and readiness probes
9. Document developer setup steps and architecture decisions
10. Configure rate limiting for /auth/login
11. Break down RssIngestService into smaller components
12. Add ProblemDetails handler to reporter-service
13. Secure H2 consoles behind profiles
14. Introduce CI pipeline enforcing tests and static analysis
15. Define API gateway to isolate internal endpoints

---

# SVENSK RAPPORT

## 1. Executive Summary

- Tjänsteflödet fungerar delvis via reporter-service men payments-xmr-service saknar API
- Reporter-service gör prenumerationskontroller via WebClient men felhantering kan läcka 502-svar
- Controller- och servicelager är separerade men undantagshantering skiljer sig mellan tjänsterna
- Validering sker ojämnt; interna API:er har @Valid men publika endpoints litar på request-attribut
- Säkerhetskonfigurationer tillåter interna vägar utan ordentlig API-nyckel
- Hårdkodade utvecklingshemligheter förekommer i properties och docker-compose
- Testsviten består bara av contextLoads
- Observability är begränsad till loggning; inga Actuator-metriker
- Docker Compose driftsätter lokalt men exponerar H2-data utan åtkomstkontroller
- README beskriver visionen men saknar konkreta installationsinstruktioner

## 2. Betygstabell (0–5)

| Område | Betyg | Kommentar |
|--------|-------|-----------|
| Arkitekturanpassning | 2 | Avsaknad av payments-API stoppar flödet |
| Clean Code & SOLID | 3 | Överskådlig kod men brist på konsekvent validering |
| REST & Spring-konventioner | 2 | Verb stämmer men felrespons varierar |
| Säkerhet & konfiguration | 2 | Interna vägar öppna, hemligheter hårdkodade |
| Testning | 1 | Endast contextLoads-tester |
| Observability & drift | 2 | Loggar finns men Actuator saknas |
| Dokumentation & DX | 3 | README ger översikt men saknar steg-för-steg |

## 3. Arkitekturanpassning

Reporter-service samlar in data och koordinerar byggande samt notifiering av rapporter genom att anropa ingest-, rapport- och notifikationskomponenter vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/ReporterCoordinator.java. Prenumerationsstatus verifieras via WebClient mot subscriptions-service innan rapporter returneras vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/SubscriptionAccessService.java. Notifikationer skickas via ReportNotificationPublisher under förutsättning att bas-URL och intern nyckel finns vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/ReportNotificationPublisher.java.

Men payments-xmr-service innehåller endast en tom Spring Boot-applikation utan endpoints vid apps/payments-xmr-service/src/main/java/com/novareport/payments_xmr_service/PaymentsXmrServiceApplication.java. Flaggor som subs.fake-all-active gör dessutom att tjänsten kan kringgås vid apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java. Interna API:er exponeras offentligt utan gateway vid apps/reporter-service/src/main/java/com/novareport/reporter_service/controller/InternalReporterController.java.

## 4. Clean Code & SOLID – Topp 10 iakttagelser

1. ReportController separerar pagination men använder generella IllegalArgumentException vid apps/reporter-service/src/main/java/com/novareport/reporter_service/controller/ReportController.java rader 63-79.
2. SubscriptionAccessService blandar tokenextraktion och nätverksanrop vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/SubscriptionAccessService.java rader 33-75.
3. RssIngestService är funktionsrik men svårtestad då fetch, parse och persist ligger i samma klass vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/RssIngestService.java rader 46-167.
4. DailyReportService använder strategier för sammanfattning – bra extensibilitet vid apps/reporter-service/src/main/java/com/novareport/reporter_service/service/DailyReportService.java rader 31-105.
5. SubscriptionController förlitar sig på attributet uid utan validering vid apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/SubscriptionController.java rader 46-56.
6. InternalSubscriptionController demonstrerar god användning av @Valid vid apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/InternalSubscriptionController.java rader 25-37.
7. NotificationsClient bygger URL-strängar manuellt vid apps/reporter-service/src/main/java/com/novareport/reporter_service/client/NotificationsClient.java rader 23-37.
8. GlobalExceptionHandler i accounts-service levererar ProblemDetail vid apps/accounts-service/src/main/java/com/novareport/accounts_service/common/GlobalExceptionHandler.java rader 15-47.
9. Reporter-service använder egen ApiError vid apps/reporter-service/src/main/java/com/novareport/reporter_service/exception/GlobalExceptionHandler.java rader 12-44.
10. SubscriptionService saknar adapterlager kring repository vid apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java rader 18-85.

## 5. REST & Spring-granskning

| Sökväg | Verb | Status | ProblemDetails | Referens |
|--------|------|--------|----------------|----------|
| /auth/register | POST | 201, 400 | Ja | apps/accounts-service AuthController |
| /auth/login | POST | 200, 401 | Ja | apps/accounts-service AuthController |
| /me | GET | 200, 500 | Nej | apps/accounts-service UserController |
| /me/settings | PUT | 200, 400 | Nej | apps/accounts-service UserController |
| /api/v1/subscriptions/me/has-access | GET | 200, 400 | Nej | apps/subscriptions-service SubscriptionController |
| /api/v1/subscriptions/me | GET | 200, 404 | Nej | apps/subscriptions-service SubscriptionController |
| /api/v1/internal/subscriptions/activate | POST | 200 | Validerad | apps/subscriptions-service InternalSubscriptionController |
| /api/v1/reports/latest | GET | 200, 404 | Nej | apps/reporter-service ReportController |
| /api/v1/reports | GET | 200, 400 | Nej | apps/reporter-service ReportController |
| /api/v1/internal/reporter/* | POST/GET | 202, 200 | Nej | apps/reporter-service InternalReporterController |
| /api/v1/notifications/latest | GET | 200, 404 | Nej | apps/notifications-service NotificationController |

Controllerdesignen saknar konsekvent @Validated och återanvänder inte ProblemDetails.

## 6. Säkerhet & konfiguration

- MEDEL: Reporter-service släpper igenom /api/v1/internal/** utan X-INTERNAL-KEY vid apps/reporter-service/src/main/java/com/novareport/reporter_service/config/SecurityConfig.java
- MEDEL: Docker Compose innehåller hårdkodade JWT-hemligheter vid deploy/docker-compose.yml
- HÖG: WebClient-anropen litar på konfigurerade bas-URL:er utan SSRF-skydd vid apps/reporter-service/src/main/java/com/novareport/reporter_service/client/SubscriptionsClient.java
- LÅG: Debug-loggning av säkerhet i dev vid apps/accounts-service/src/main/resources/application-dev.properties
- MEDEL: CSRF är avstängt vid apps/accounts-service/src/main/java/com/novareport/accounts_service/config/SecurityConfig.java
- MEDEL: subs.fake-all-active kan kringgå åtkomstkontroll vid apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/service/SubscriptionService.java
- HÖG: Ingen spärr mot brute-force på /auth/login vid apps/accounts-service/src/main/java/com/novareport/accounts_service/auth/AuthController.java

## 7. Testluckor

- AuthControllerLogin_SucceedsWithValidCredentials – säkerställ token och aktivitetslogg
- AuthControllerRegister_FailsOnDuplicateEmail – kontrollera ProblemDetail
- SubscriptionServiceHasAccess_RespectsActiveWindow – testa flaggor och repositoryfrågor
- ReportControllerList_RejectsInvalidPagination – verifiera felrespons
- SubscriptionAccessServiceAssertAccess_HandlesDownstreamFailures – mocka WebClient för felvägar
- ReportNotificationPublisherPublish_SkipsWithoutConfig – testa skip-logik
- NotificationControllerReportReady_ValidatesPayload – verifiera @Valid-respons

## 8. Observability & drift

- Befintligt: Loggning med kontext i ingest- och notifieringsflöden
- Befintligt: Docker Compose kopplar samman tjänster i ett nätverk
- Saknas: Actuator-metriker och readiness-prober
- Saknas: Gemensam logg- eller trace-korrelation
- Saknas: Health checks mot databaser och externa API:er
- Saknas: Strukturerad loggning (JSON)
- Saknas: Observability kring WebClient

## 9. Åtgärdslista

1. Bygg ut payments-xmr-service med endpoints som aktiverar prenumerationer
2. Kräv och validera X-INTERNAL-KEY för alla interna vägar
3. Ta bort hårdkodade hemligheter och använd enbart miljövariabler
4. Standardisera felhantering till RFC 7807 i alla tjänster
5. Lägg till valideringslager och tydliga DTO:er för samtliga controllers
6. Inför enhetstester och integrationstester för kärnlogik
7. Förstärk WebClient med skydd mot ogiltiga URL:er, timeouts och retries
8. Aktivera Spring Actuator med hälsa, metrics och readiness
9. Uppdatera README med installationssteg och testkommandon
10. Inför rate limiting för inloggningsförsök
11. Refaktorera RssIngestService till mindre testbara komponenter
12. Harmoniera exception handlers över tjänsterna
13. Stäng eller skydda H2-konsoler i byggda bilder
14. Etablera CI som kör tester och statisk analys
15. Definiera ingress/gateway som isolerar interna endpoints

