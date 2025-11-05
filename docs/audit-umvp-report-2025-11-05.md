# Nova Report UMVP – Code Audit Report
**Date:** 2025-11-05  
**Auditor:** Claude Sonnet 4.5

---

# ENGLISH REPORT

## Executive Summary
- Architecture follows UMVP design with clear service boundaries
- **payments-xmr-service is empty** – only contains main class
- **HIGH RISK:** Hardcoded secrets in dev configs and docker-compose.yml
- No real tests – only smoke tests exist
- Good naming, but SRP violations in coordinators
- REST correct but inconsistent Problem Details
- Minimal observability setup
- Missing service documentation
- **UMVP goal achieved:** Reporter builds reports without AI

## Scores (0–5)
| Area | Score | Comment |
|------|-------|---------|
| Architecture Fit | 3 | Clear boundaries, payments service empty |
| Clean Code & SOLID | 3 | Good naming, SRP violations |
| REST & Spring | 3 | Correct verbs, inconsistent errors |
| Security & Config | 2 | Hardcoded secrets (HIGH RISK) |
| Testing | 1 | Only smoke tests |
| Observability | 2 | Actuator minimal |
| Documentation | 2 | Root README clear, service docs missing |

## Architecture
**Intended:** frontend → accounts → subscriptions → payments → reporter → notifications  
**Actual:** Works except payments-xmr-service empty. Frontend uses notifications-service for reports. No scheduled jobs.

## Clean Code Top 10
1. ✅ Clear DTO naming
2. ✅ Lombok reduces boilerplate
3. ❌ ReporterCoordinator violates SRP
4. ❌ RssIngestService.ingest() is 100+ lines
5. ✅ Interface segregation in DailyReportService
6. ❌ Magic strings in AuthController
7. ❌ Inconsistent exceptions
8. ✅ Constructor injection
9. ❌ Missing validation on entities
10. ✅ Java records for DTOs

## Security Findings
- **HIGH:** Hardcoded JWT secret in application-dev.properties
- **HIGH:** Hardcoded internal API key in configs
- **HIGH:** Secrets in docker-compose.yml
- **MEDIUM:** Weak internal API key validation
- **MEDIUM:** No rate limiting
- **MEDIUM:** No password validation

## Missing Tests
1. AuthControllerTest.shouldReturn409WhenEmailExists
2. JwtServiceTest.shouldRejectExpiredToken
3. SubscriptionServiceTest.shouldExtendExisting
4. RssIngestServiceTest.shouldDeduplicateByHash
5. DailyReportServiceTest.shouldUseFakeSummary
6. InternalApiKeyFilterTest.shouldReturn403

## Observability
**Exists:** Actuator health, SLF4J logging, Dockerfiles  
**Missing:** Health checks in compose, structured logging, metrics, tracing

## Action Checklist
1. Implement payments-xmr-service
2. Remove all hardcoded secrets
3. Fix API key filter (fail-closed)
4. Add unit tests (70%+ coverage)
5. Implement RFC 7807 Problem Details
6. Add password validation
7. Add rate limiting
8. Add @Scheduled tasks
9. Add health checks to compose
10. Enable JSON logging
11. Expose metrics endpoint
12. Add integration tests
13. Document APIs with OpenAPI
14. Create service READMEs
15. Add prod configuration

---

# SVENSK RAPPORT

## Sammanfattning
- Arkitekturen följer UMVP-design med tydliga servicegränser
- **payments-xmr-service är tom** – innehåller bara main-klass
- **HÖG RISK:** Hårdkodade hemligheter i dev-configs och docker-compose.yml
- Inga riktiga tester – endast smoke-tester finns
- Bra namngivning, men SRP-brott i koordinatorer
- REST korrekt men inkonsekvent Problem Details
- Minimal observerbarhetsinställning
- Saknar servicedokumentation
- **UMVP-mål uppnått:** Reporter bygger rapporter utan AI

## Poäng (0–5)
| Område | Poäng | Kommentar |
|--------|-------|-----------|
| Arkitektur | 3 | Tydliga gränser, payments-service tom |
| Clean Code & SOLID | 3 | Bra namngivning, SRP-brott |
| REST & Spring | 3 | Korrekta verb, inkonsekvent fel |
| Säkerhet & Config | 2 | Hårdkodade hemligheter (HÖG RISK) |
| Testning | 1 | Endast smoke-tester |
| Observerbarhet | 2 | Actuator minimal |
| Dokumentation | 2 | Root README tydlig, service-docs saknas |

## Arkitektur
**Avsett:** frontend → accounts → subscriptions → payments → reporter → notifications  
**Faktiskt:** Fungerar förutom att payments-xmr-service är tom. Frontend använder notifications-service för rapporter. Inga schemalagda jobb.

## Clean Code Topp 10
1. ✅ Tydlig DTO-namngivning
2. ✅ Lombok minskar boilerplate
3. ❌ ReporterCoordinator bryter SRP
4. ❌ RssIngestService.ingest() är 100+ rader
5. ✅ Interface segregation i DailyReportService
6. ❌ Magic strings i AuthController
7. ❌ Inkonsekvent undantag
8. ✅ Konstruktorinjektion
9. ❌ Saknade valideringar på entiteter
10. ✅ Java records för DTOs

## Säkerhetsfynd
- **HÖG:** Hårdkodad JWT-hemlighet i application-dev.properties
- **HÖG:** Hårdkodad internal API-nyckel i configs
- **HÖG:** Hemligheter i docker-compose.yml
- **MEDEL:** Svag internal API-nyckelvalidering
- **MEDEL:** Ingen rate limiting
- **MEDEL:** Ingen lösenordsvalidering

## Saknade tester
1. AuthControllerTest.shouldReturn409WhenEmailExists
2. JwtServiceTest.shouldRejectExpiredToken
3. SubscriptionServiceTest.shouldExtendExisting
4. RssIngestServiceTest.shouldDeduplicateByHash
5. DailyReportServiceTest.shouldUseFakeSummary
6. InternalApiKeyFilterTest.shouldReturn403

## Observerbarhet
**Finns:** Actuator health, SLF4J-loggning, Dockerfiles  
**Saknas:** Health checks i compose, strukturerad loggning, metrics, tracing

## Åtgärdschecklista
1. Implementera payments-xmr-service
2. Ta bort alla hårdkodade hemligheter
3. Fixa API-nyckelfilter (fail-closed)
4. Lägg till enhetstester (70%+ täckning)
5. Implementera RFC 7807 Problem Details
6. Lägg till lösenordsvalidering
7. Lägg till rate limiting
8. Lägg till @Scheduled-tasks
9. Lägg till health checks i compose
10. Aktivera JSON-loggning
11. Exponera metrics-endpoint
12. Lägg till integrationstester
13. Dokumentera APIs med OpenAPI
14. Skapa service-READMEs
15. Lägg till prod-konfiguration
