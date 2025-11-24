 # Nova Report – UMVP Architecture & Code Audit Report (2025-11-23)

 ## Svensk rapport

 ### 1. Sammanfattning

 - Arkitekturen följer i stort den tänkta mikroservice-modellen i docs/service-overviews.md: varje tjänst har tydligt ansvar (konton, prenumerationer, betalningar, rapporter, notifieringar, frontend) och egen databas.
 - Säkerheten är god för en UMVP: gemensam JWT-konfiguration, stateless Spring Security, interna API-nycklar och rate limiting på login i apps/accounts-service.
 - REST-API:erna använder konsekventa, versionerade URL:er och rimliga HTTP-verb; alla backends har centraliserad felhantering via ProblemDetail-baserade GlobalExceptionHandler-klasser.
 - Monero-betalningsflödet är implementerat end-to-end i apps/payments-xmr-service med aktivering av prenumerationer via apps/subscriptions-service.
 - Observability är väl genomtänkt: CorrelationIdFilter i alla backends, logback-spring.xml med correlationId i logg-mönstret och Actuator-health kopplad till Docker-healthchecks.
 - Backend-testerna har stärkts med JaCoCo-regler (minst 80% linjetäckning) och många *Test.java-klasser per tjänst; däremot saknas fortfarande fullständiga end-to-end-tester och frontend-tester för hela flödet.
 - Konfigurationen följer 12-Factor-tänket via miljövariabler i deploy/docker-compose.prod.yml och deploy/.env.example; deploy/.env innehåller dock riktiga hemligheter som måste hanteras mycket försiktigt.
 - Kodstilen är överlag ren med tunna controllers, tydliga services och DTOs; viss duplicering finns kvar i JWT-hantering och säkerhetsfilter mellan tjänster.
 - Dokumentation och DX är starka tack vare README.md, README-CODE-QUALITY.md och detaljerad tjänst-specifik dokumentation i docs/ (t.ex. payments-xmr-service-guide, notifications-service, observability-tracing, service-overviews).
 - Helhetsintrycket är att NovaReport nu ligger stabilt över en "godkänd UMVP" för ett examensarbete; nästa steg är främst att konsolidera säkerhetsmönster, stärka hemlighetshantering, lägga till cross-service-tester och bygga ut metrics/tracing.

 ### 2. Poängtabell (0–5)

 | Område | Poäng | Kommentar |
 |--------|-------|-----------|
 | Arkitektur | 4 | Tjänster och flöden matchar dokumenterad arkitektur med tydliga gränser och separata databaser. |
 | Clean Code & SOLID | 4 | Överlag små, fokuserade klasser och DTOs; viss duplicering av JWT/säkerhet kvarstår. |
 | REST & Spring | 4 | Bra endpoints, validering och ProblemDetail-felmodell; interna API:er avgränsade via path-prefix. |
 | Säkerhet & konfig | 4 | Stabil grund med JWT, interna nycklar, rate limiting och miljövariabler; hemligheter och CORS kan ytterligare skärpas. |
 | Testning | 4 | Stark backend-täckning genom JaCoCo-krav och många tester; begränsad E2E- och frontend-testning. |
 | Observability & drift | 4 | Korrelations-ID, healthchecks och loggstruktur finns; ingen central metrics-/tracing-stack ännu. |
 | Dokumentation & DX | 4 | Bra helhetsdokumentation och check-skript; operativa runbooks är fortfarande tunna. |

 ### 3. Arkitektur

 - **accounts-service**
   - Ansvarar för registrering och login samt JWT-utgivning.
   - AuthController (apps/accounts-service/src/main/java/com/novareport/accounts_service/auth/AuthController.java) hanterar /auth/register och /auth/login.
   - UserController (apps/accounts-service/src/main/java/com/novareport/accounts_service/user/UserController.java) exponerar /api/accounts/me och /api/accounts/me/settings.
   - InternalUserSettingsController (apps/accounts-service/src/main/java/com/novareport/accounts_service/settings/InternalUserSettingsController.java) exponerar /api/accounts/internal/report-email-subscribers för interna samtal från notifications-service.
   - JWT-konfigurationen finns i application.properties (jwt.secret via JWT_SECRET, jwt.issuer via JWT_ISSUER) och följer docs/jwt-conventions.md.

 - **subscriptions-service**
   - Håller reda på vilka användare som har aktiv prenumeration.
   - SubscriptionController (apps/subscriptions-service/src/main/java/com/novareport/subscriptions_service/controller/SubscriptionController.java) exponerar /api/v1/subscriptions/me/has-access och /api/v1/subscriptions/me.
   - InternalSubscriptionController exponerar /api/v1/internal/subscriptions/activate, /cancel och /active-users för interna anrop från payments-xmr-service och notifications-service.
   - JwtService (i denna tjänst) läser och validerar JWT enligt samma konventioner som i övriga backends.

 - **payments-xmr-service**
   - Implementerar Monero-betalningar enligt docs/payments-xmr-service-guide.md.
   - PaymentController (apps/payments-xmr-service/src/main/java/com/novareport/payments_xmr_service/controller/PaymentController.java) exponerar /api/v1/payments/create och /api/v1/payments/{paymentId}/status för frontend.
   - InternalPaymentController exponerar /api/v1/internal/payments/{paymentId}/confirm för manuella eller administrativa bekräftelser.
   - SubscriptionsClient (apps/payments-xmr-service/src/main/java/com/novareport/payments_xmr_service/service/SubscriptionsClient.java) anropar /api/v1/internal/subscriptions/activate i subscriptions-service via SUBSCRIPTIONS_BASE_URL.
   - Monero-wallet-rpc körs som egen container i deploy/docker-compose*.yml och nås endast internt.

 - **reporter-service**
   - Ansvarar för att hämta nyheter, generera och lagra rapporter samt exponera dem till klienter.
   - ReportController (apps/reporter-service/src/main/java/com/novareport/reporter_service/controller/ReportController.java) exponerar /api/v1/reports/latest samt en paginerad lista på /api/v1/reports.
   - SubscriptionAccessService kontrollerar åtkomst mot subscriptions-service innan rapporter returneras.
   - Schemaläggning och AI-integration följer docs/scheduler-implementation.md och docs/onemin-ai-integration.md.

 - **notifications-service**
   - Tar emot "report ready"-notiser från reporter-service och skickar rapporter via e-post och Discord enligt docs/notifications-service.md.
   - NotificationController (apps/notifications-service/src/main/java/com/novareport/notifications_service/controller/NotificationController.java) exponerar /api/v1/internal/notifications/report-ready och /api/v1/notifications/latest.
   - E-post och Discord styrs via miljövariabler i deploy/.env(.example) som mappas till notifications.mail.* och notifications.discord.* properties.

 - **frontend**
   - Byggd i React + TypeScript (apps/frontend) och pratar med backends via VITE_* variabler som sätts i deploy/docker-compose.yml.
   - Fokuserar på användarflödet: registrering/login, köpa prenumeration med Monero, se access-status och läsa senaste rapporten.

 - **Infrastruktur (deploy/)**
   - deploy/docker-compose.yml definierar en dev-stack med alla tjänster och monero-wallet-rpc i ett gemensamt nätverk.
   - deploy/docker-compose.prod.yml definierar produktionsliknande miljö med postgres som gemensam DB-backend men separata databaser per tjänst (ACCOUNTS_DB_NAME, SUBSCRIPTIONS_DB_NAME, PAYMENTS_DB_NAME, REPORTER_DB_NAME, NOTIFICATIONS_DB_NAME).
   - Hälsokontroller via /actuator/health används som Docker-healthchecks för centrala backends.

 ### 4. Clean Code & SOLID – 10 observationer

 1. **AuthController i accounts-service**
    - Tydlig separation mellan register och login, med hjälpmetoder för att skapa User, UserSettings och ActivityLog.
    - Micrometer används för att mäta login-latens och antal försök, vilket är bra både ur observability- och säkerhetsperspektiv.

 2. **RateLimitFilter i accounts-service**
    - RateLimitingConfig + RateLimitFilter (apps/accounts-service/src/main/java/com/novareport/accounts_service/filter/RateLimitFilter.java) begränsar antal login/registreringsförsök per IP.
    - Ger ett fokuserat cross-cutting-lager som minskar brute-force-risk utan att smutsa ned controllers.

 3. **SubscriptionController i subscriptions-service**
    - Använder en separat hjälpfunktion resolveUserId för att läsa uid från request-attribut (satt av JwtAuthenticationFilter).
    - Returnerar HasAccessResponse och SubscriptionResponse via Optional-kedjor, vilket håller HTTP-lagret rent från domänlogik.

 4. **InternalSubscriptionController**
    - Avgränsar interna operations (activate, cancel, active-users) till /api/v1/internal/subscriptions/**.
    - Ger en tydlig yta för inter-service-kommunikation från payments-xmr-service och notifications-service.

 5. **PaymentController i payments-xmr-service**
    - Håller sig nära HTTP-ansvaret: läser userId via RequestUtils.resolveUserId, loggar med LogSanitizer och delegerar resten till PaymentService.
    - Endpoints (/create och /{id}/status) är enkla att konsumera från frontend.

 6. **GlobalExceptionHandler i alla backends**
    - accounts-service har en ControllerAdvice-baserad GlobalExceptionHandler som mappar valideringsfel och egna AccountsException till ProblemDetail.
    - subscriptions-, payments-, reporter- och notifications-service använder @RestControllerAdvice + ProblemDetail med tydliga titlar (t.ex. "Validation Failed", "Payment Not Found").
    - Ger en konsekvent felmodell över tjänsterna.

 7. **CorrelationIdFilter + logback-konfiguration**
    - Varje backend har CorrelationIdFilter som läser/äger X-Correlation-ID och skriver det till MDC.
    - logback-spring.xml i varje backend inkluderar [%X{correlationId}] i logg-mönstret.
    - Skapar bra förutsättningar för felsökning över tjänstegränser (beskrivet i docs/observability-tracing.md).

 8. **SecurityConfig i accounts- och notifications-service**
    - SecurityConfig i apps/accounts-service och apps/notifications-service bygger upp en stateless SecurityFilterChain med JwtAuthenticationFilter och InternalApiKeyFilter.
    - Vanliga paths för dokumentation, /actuator/health, H2-console m.m. är tydligt undantagna, resten kräver authentication.

 9. **Teststruktur och JaCoCo-konfiguration**
    - samtliga backend-pom.xml innehåller jacoco-maven-plugin med BUNDLE-regel och minimum 0.80 LINE COVEREDRATIO.
    - find_by_name visar ett stort antal *Test.java-klasser i varje tjänst (controllers, filters, security, jobs, domänklasser).
    - Detta driver fram relativt hög kodkvalitet och minskar risken för regressionsfel.

 10. **Duplicering i JWT- och säkerhetskod**
     - Varje backend har egen JwtService och JwtAuthenticationFilter-variant som följer docs/jwt-conventions.md.
     - Mönstret är konsekvent, men innebär att framtida ändringar (nya claims, ändrade TTL:er) måste upprepas i flera tjänster.

 ### 5. REST & Spring

 - **URL-struktur och verb**
   - /auth/** för registrering/login, /api/v1/subscriptions/** för prenumerationer, /api/v1/payments/** för betalningar, /api/v1/reports/** för rapporter och /api/v1/notifications/** för notifieringar.
   - Interna endpoints ligger under /api/accounts/internal/** och /api/v1/internal/**-prefix i respektive tjänst.

 - **Validering**
   - @Valid används på request bodies i t.ex. AuthController, InternalSubscriptionController, PaymentController och NotificationController.
   - Bean Validation-annoteringar på DTOs ger tydliga fältfel som sedan packas in i ProblemDetail-svar.

 - **Felmodell**
   - GlobalExceptionHandler-klasser konverterar valideringsfel, ResponseStatusException, IllegalArgumentException och domänspecifika exceptions (t.ex. PaymentNotFoundException, InvalidPaymentStateException, SubscriptionActivationException) till ProblemDetail med lämplig HTTP-status.
   - Detta ger en mer förutsägbar felhantering än Springs standardfel.

 - **Responsmodeller**
   - DTOs (AuthResponse, HasAccessResponse, SubscriptionResponse, CreatePaymentResponse, PaymentStatusResponse, NotificationReportResponse, DailyReportResponse m.fl.) används konsekvent i stället för att exponera entiteter direkt.

 ### 6. Säkerhet & konfiguration

 - **JWT**
   - Alla backends använder JWT_SECRET och JWT_ISSUER från miljövariabler (se deploy/.env.example och respektive application.properties).
   - docs/jwt-conventions.md beskriver gemensamt format (HS256, iss, sub, uid, role, exp) och används som referens.

 - **Interna API-nycklar**
   - INTERNAL_API_KEY används i deploy/.env(.example) och mappas till internal.api-key i tjänsterna.
   - InternalApiKeyFilter-varianter skyddar interna endpoints (t.ex. /api/v1/internal/subscriptions/**, /api/v1/internal/payments/**, /api/v1/internal/notifications/**).
   - Nyckeln får aldrig läcka till frontend (endast backend och Compose-konfiguration ska känna till den).

 - **Rate limiting**
   - RateLimitingConfig + RateLimitFilter i accounts-service begränsar antal försök mot /auth/login och /auth/register per IP.
   - Minskar risken för brute-force och credential stuffing mot login.

 - **CORS**
   - CORS-konfigurationen hanteras via CorsConfigurationSource och CORS_ALLOWED_ORIGINS (i deploy/.env).
   - För produktion rekommenderas att begränsa CORS_ALLOWED_ORIGINS till kända frontend-domäner (t.ex. nova.drillbi.se).

 - **Konfiguration & hemligheter**
   - deploy/.env.example visar en ren, generisk konfiguration utan riktiga hemligheter.
   - deploy/.env innehåller däremot riktiga värden för JWT_SECRET, SMTP, Discord-webhook och externa API-nycklar.
   - Dessa får inte spridas utanför säkra miljöer och bör roteras om det finns risk för exponering.

 - **Monero-specifika inställningar**
   - MONERO_DAEMON_ADDRESS, MONERO_WALLET_NAME, MONERO_WALLET_PASSWORD och MONERO_MIN_CONFIRMATIONS sätts via deploy/.env och används i både dev- och prod-compose.
   - Kommentaren i .env påpekar att MONERO_MIN_CONFIRMATIONS bör höjas för "real world application" – viktigt att följa vid verklig drift.

 ### 7. Testluckor

 Trots god backend-täckning finns flera intressanta luckor:

 - **Cross-service-flöden**
   - Inga automatiska tester som täcker hela vägen: skapa betalning i payments-xmr-service → bekräfta betalning → aktivera prenumeration i subscriptions-service → läsa rapport i reporter-service.

 - **JwtService edge cases**
   - Det vore värdefullt med fler tester för felaktiga/expirerade tokens och fel issuer i payments-xmr-service och notifications-service, med fokus på att rätt ProblemDetail returneras.

 - **Scheduler- och jobbfel**
   - notifications-service har jobb för e-post och Discord; fler tester runt felvägar (ingen rapport, SMTP-fel, Discord-fel) skulle höja robustheten.

 - **Frontend**
   - apps/frontend har stöd för vitest m.m., men det saknas tydliga tester för kritiska flöden (login, prenumeration, betalningsstatus, läs rapport).

 ### 8. Observability & drift

 - **Korrelations-ID**
   - CorrelationIdFilter finns i accounts-, subscriptions-, payments-xmr-, reporter- och notifications-service.
   - Varje filter läser X-Correlation-ID (eller skapar ett nytt UUID) och skriver det både till MDC (correlationId) och till svarshuvudet.
   - logback-spring.xml i varje backend loggar [%X{correlationId}], precis som beskrivet i docs/observability-tracing.md.

 - **Health checks**
   - Spring Boot Actuator-health är aktiverat och exponeras på /actuator/health.
   - deploy/docker-compose.prod.yml kopplar healthchecks till dessa endpoints för samtliga centrala backends.

 - **Metrics**
   - AuthController använder Micrometer för login-latens och antal försök.
   - Det finns förutsättningar att bygga vidare med fler metrics (betalningar, rapportgenerering, utskick), men ingen central metrics-stack är definierad i deploy/.

 - **Loggning**
   - Kodkvalitetsrapporten (code-quality-report.txt) visar att SpotBugs har flaggat potentiella log-injection-problem i payments-xmr-service.
   - LogSanitizer används där för att sanera värden innan de loggas, men varningar bör ändå följas upp regelbundet.

 ### 9. Åtgärdslista

 1. **Säkerställ hemlighetshantering för produktion**
    - Behandla deploy/.env som strikt dev/test.
    - Rotera JWT-, SMTP-, Discord- och externa API-nycklar om de kan ha läckt.
    - Använd hemlighetshanterare eller miljövariabler på servernivå i stället för delade filer.

 2. **Begränsa CORS i produktion**
    - Sätt CORS_ALLOWED_ORIGINS till kända frontend-domäner och dokumentera rekommenderade värden i docs/.

 3. **Standardisera JWT- och säkerhetsmönster**
    - Extrahera gemensam JwtService/JwtAuthenticationFilter till en liten shared-modul eller dokumentera en tydlig kodmall som alla tjänster följer.

 4. **Stärk cross-service-testning**
    - Lägg till integrationstester som täcker betalning→prenumeration→rapportflödet (gärna med Testcontainers eller liknande).

 5. **Bygg ut frontend-testning**
    - Lägg till vitest-baserade tester för de viktigaste UI-flödena: registrering, login, köp, betalningsstatus, läs rapport.

 6. **Förbättra observability**
    - Inför en enkel metrics-/tracing-stack (t.ex. Prometheus + Grafana, eller OpenTelemetry) och exponera befintliga Micrometer-metrics dit.

 7. **Dokumentera operativa rutiner**
    - Skapa korta runbooks i docs/ för nyckelscenarier: Monero-nedtid, nyckelrotation, SMTP/Discord-problem, manuella betalningsbekräftelser.

 8. **Följ upp statisk analys (SpotBugs/FindSecBugs)**
    - Gå igenom varningar i code-quality-report.txt för payments-xmr-service.
    - Dokumentera vilka varningar som är åtgärdade respektive medvetet accepterade (med motivering).

