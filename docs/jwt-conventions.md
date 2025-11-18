# JWT Conventions for Nova Report

This document defines how JSON Web Tokens (JWT) are issued and validated across all Nova Report backend services.

## 1. Environment Variables

All services share the same JWT-related configuration, provided via environment variables:

- `JWT_SECRET` – Symmetric signing key used for HS256.
- `JWT_ISSUER` – Expected issuer for all access tokens (currently `accounts-service`).
- `JWT_ACCESS_TOKEN_MINUTES` – Access token lifetime in minutes (e.g. `30`).

Spring properties mapping:

- `jwt.secret = ${JWT_SECRET}`
- `jwt.issuer = ${JWT_ISSUER}`
- `jwt.access-token-minutes = ${JWT_ACCESS_TOKEN_MINUTES}`

## 2. Access Token Format

All access tokens share the same structure:

- **Algorithm**: `HS256`
- **Issuer (`iss`)**: value of `JWT_ISSUER` (e.g. `accounts-service`)
- **Subject (`sub`)**: user email address
- **Claims**:
  - `uid` – user ID as a UUID string
  - `role` – user role, e.g. `USER` or `ADMIN`
- **Expiration (`exp`)**: `now + JWT_ACCESS_TOKEN_MINUTES`

Example payload (illustrative only):

```json
{
  "iss": "accounts-service",
  "sub": "user@example.com",
  "uid": "550e8400-e29b-41d4-a716-446655440000",
  "role": "USER",
  "iat": 1731896400,
  "exp": 1731898200
}
```

## 3. Authorization Header

Clients and internal services send access tokens using the HTTP Authorization header:

- `Authorization: Bearer <access_token>`

No other authentication headers should be used for JWT authentication.

## 4. Issuing Tokens

### Responsibility

- **accounts-service** is the single source of truth for issuing user access tokens.
- Other services should not mint user access tokens in normal flows.

### Implementation

`accounts-service` uses `JwtService.createAccessToken(UUID userId, String email, String role)` with the conventions above:

- `sub` = `email`
- `uid` = `userId.toString()`
- `role` = role string

The resulting token is returned to the frontend on successful login/registration.

## 5. Validating Tokens

All backend services validate access tokens using the same rules:

1. **Signature**
   - Verify HS256 signature with `JWT_SECRET`.
2. **Issuer**
   - Require `iss == JWT_ISSUER`.
3. **Expiration**
   - Reject tokens where `exp` is in the past.
4. **Claims**
   - Expect `uid` and `role` claims to be present when user context is needed.

In practice this is implemented via each service's `JwtService.parse(...)` or equivalent method, which:

- Uses `jwt.secret` to configure the signing key.
- Calls `requireIssuer(jwt.issuer)` during parsing.

The `payments-xmr-service` `JwtService` uses the newer JJWT API (`verifyWith(...).parseSignedClaims(...)`), but the semantics (key + issuer + expiry) are identical.

## 6. Using Claims in Services

When a service needs the current user ID or role, it should:

- Extract the token from the `Authorization` header.
- Delegate to its local `JwtService` to validate and parse the token.
- Read claims from the resulting `Claims` object, e.g.:
  - `uid` → user ID (UUID as string)
  - `role` → user role

No service should depend on additional custom claims without updating this document.

## 7. Future Improvements

If the project grows, JWT handling can be further standardized by introducing a small shared library, for example:

- `JwtTokenFactory` – issues tokens given `userId`, `email`, `role`, `secret`, `issuer`, and `ttl`.
- `JwtTokenParser` – validates and parses tokens, enforcing the conventions in this document.

For the UMVP, the documented conventions above are sufficient as long as all services keep their `JwtService` implementations aligned with this specification.
