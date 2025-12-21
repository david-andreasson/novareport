# Observability & tracing i NovaReport

Det här dokumentet är mina egna anteckningar om hur jag har satt upp observability och enkel request-tracing i UMVP-versionen av NovaReport.

## Mål

- Göra det enkelt för mig att följa ett enskilt anrop genom flera tjänster via loggar.
- Ge konsekventa fel-svar till klienter.
- Exponera grundläggande hälsostatus för drift och Docker-orchestrering.

## Correlation IDs

### Översikt

Alla backend-tjänster använder ett korrelations-ID för att knyta ihop loggrader som hör till samma logiska anrop.

Obs: `payments-stripe-service` saknar i nuläget ett inkommande `CorrelationIdFilter` som sätter MDC för varje request. Däremot propagerras `X-Correlation-ID` på utgående interna anrop när ett `correlationId` finns i MDC.

- Header-namn: `X-Correlation-ID`
- MDC-nyckel: `correlationId`

### Hur det fungerar

1. **Inkommande anrop**
   - Alla Spring Boot-tjänster (utom `payments-stripe-service`) har ett `CorrelationIdFilter` (en `OncePerRequestFilter`).
   - Filtret läser `X-Correlation-ID` från inkommande HTTP-anrop.
   - Om headern saknas eller är tom genereras ett nytt UUID.
   - Värdet skrivs till SLF4J MDC under nyckeln `correlationId`.
   - Samma värde sätts även på HTTP-svaret i headern `X-Correlation-ID`.

2. **Loggkonfiguration**
   - Varje tjänst har en `logback-spring.xml` som inkluderar MDC-nyckeln i loggmönstret, till exempel:
     - `[%X{correlationId}]` renderas i varje loggrad.

3. **Interna HTTP-anrop**
   - När en tjänst anropar en annan intern tjänst propagerras det aktuella korrelations-ID:t:
     - **reporter-service → subscriptions-service**: `SubscriptionsClient` läser `MDC.get("correlationId")` och sätter `X-Correlation-ID` på det utgående anropet.
     - **reporter-service → notifications-service**: `NotificationsClient` gör samma sak när den anropar `/api/v1/internal/notifications/report-ready`.
     - **payments-xmr-service → subscriptions-service**: `SubscriptionsClient` skickar med `X-Correlation-ID` tillsammans med `X-INTERNAL-KEY`.
   - På så sätt kan jag följa en enskild användarhandling (t.ex. betalningsbekräftelse eller rapportgenerering) genom flera tjänster med samma korrelations-ID.

### Hur jag använder det i praktiken

När jag felsöker ett flöde brukar jag:

1. Ta `X-Correlation-ID` från ett klientsvar eller gateway-logg.
2. Söka efter det värdet i loggarna för alla tjänster.
3. Följa händelsekedjan end-to-end (accounts → subscriptions → payments → reporter → notifications).

## Felhantering med Problem Details

Alla externa API:er använder centraliserad felhantering baserad på Springs `ProblemDetail` (RFC 7807-stil).

- Varje tjänst har en `GlobalExceptionHandler` annoterad med `@RestControllerAdvice`.
- Typiska mappingar:
  - Valideringsfel (`MethodArgumentNotValidException`) → HTTP 400 med titeln `Validation Failed` och detaljerade fältfel.
  - Domänfel (t.ex. ogiltigt betalningstillstånd, ogiltig plan, saknad betalning) → lämplig HTTP-status (404, 400, 409) med en specifik titel.
  - Generella fel → HTTP 500 med titeln `Internal Server Error` och ett generiskt felmeddelande.
- Detta ger ett konsekvent felformat mellan tjänsterna och gör det enklare för klienten att hantera fel.

## Hälsokontroller

- Spring Boot Actuator-hälsokontroller (`/actuator/health`) är aktiverade i backend-tjänsterna.
- Docker Compose och produktions-Compose-filerna kopplar dessa kontroller till container-hälsokontroller.
  - Det gör att orchestratorn automatiskt kan starta om ohälsosamma containers.

## Framtida förbättringar

I UMVP:en har jag med flit hållit observability ganska enkel. Framöver kan jag bygga vidare med t.ex.:

- Lägga till grundläggande metrics (antal anrop, felrate, bekräftade Monero-betalningar, antal genererade rapporter) och exponera dem via Actuator/Prometheus.
- Införa ett distribuerat tracing-system (t.ex. OpenTelemetry, Zipkin eller Jaeger) och propagagera trace/span-ID:n tillsammans med korrelations-ID:t.
- Standardisera strukturerad loggning (t.ex. JSON-loggar med gemensamma fält) för enklare insamling i loggaggregat.
- Dokumentera operativa "playbooks" (nyckelrotation, hantering av Monero-node-nedtid, delvisa tjänstefel) som stöd för en mer produktionslik drift.
