# Observability & Tracing in Nova Report

This document summarizes how observability and basic request tracing are implemented in the Nova Report UMVP.

## Goals

- Make it easy to follow a single request across services via logs.
- Provide consistent error responses to clients.
- Expose basic health information for operations and Docker orchestration.

## Correlation IDs

### Overview

All backend services use a correlation ID to tie together log entries that belong to the same logical request.

- Header name: `X-Correlation-ID`
- MDC key: `correlationId`

### How it works

1. **Incoming requests**
   - Each Spring Boot service has a `CorrelationIdFilter` (a `OncePerRequestFilter`).
   - The filter reads `X-Correlation-ID` from the incoming HTTP request.
   - If the header is missing or blank, a new UUID is generated.
   - The value is written to SLF4J MDC under the key `correlationId`.
   - The same value is added to the HTTP response header `X-Correlation-ID`.

2. **Logging configuration**
   - Each service has a `logback-spring.xml` that includes the MDC key in the log pattern, for example:
     - `[%X{correlationId}]` is rendered in every log line.

3. **Internal HTTP calls**
   - When a service calls another internal service, it propagates the current correlation ID:
     - **reporter-service → subscriptions-service**: `SubscriptionsClient` reads `MDC.get("correlationId")` and sets `X-Correlation-ID` on the outgoing request.
     - **reporter-service → notifications-service**: `NotificationsClient` does the same when calling `/api/v1/internal/notifications/report-ready`.
     - **payments-xmr-service → subscriptions-service**: `SubscriptionsClient` includes `X-Correlation-ID` alongside `X-INTERNAL-KEY`.
   - This means a single user action (e.g. payment confirmation, report generation) can be followed across multiple services using the same correlation ID.

### How to use it in practice

- When debugging a flow:
  1. Take the `X-Correlation-ID` value from a client response or gateway log.
  2. Search logs in all services for that value.
  3. Follow the sequence of events end-to-end (accounts → subscriptions → payments → reporter → notifications).

## Error Handling with Problem Details

All external APIs now use centralized error handling based on Spring "ProblemDetail" (RFC 7807 style).

- Each service has a `GlobalExceptionHandler` annotated with `@RestControllerAdvice`.
- Typical mappings:
  - Validation errors (`MethodArgumentNotValidException`) → HTTP 400 with a `Validation Failed` title and detailed field messages.
  - Domain errors (e.g. invalid payment state, invalid plan, missing payment) → appropriate HTTP status (404, 400, 409) with a specific title.
  - Generic errors → HTTP 500 with `Internal Server Error` title and a generic detail message.
- This provides a consistent error format across services and simplifies client handling.

## Health Checks

- Spring Boot Actuator health endpoints (`/actuator/health`) are enabled in backend services.
- Docker Compose and production Compose files wire these health endpoints into container health checks.
  - This allows the orchestrator to restart unhealthy containers automatically.

## Future Improvements

The current UMVP intentionally keeps observability lightweight. Suggested next steps:

- Add basic metrics (request counts, error rates, Monero payment confirmations, report generations) and expose them via Actuator/Prometheus.
- Introduce a distributed tracing system (e.g. OpenTelemetry, Zipkin, or Jaeger) and propagate trace/span IDs alongside correlation IDs.
- Standardize structured logging (e.g. JSON logs with common fields) for easier ingestion into log aggregation tools.
- Document operational playbooks (key rotation, handling Monero node outages, partial service failures) to support production operations.
