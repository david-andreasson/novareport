# JWT-konventioner för NovaReport

Det här dokumentet är mina egna anteckningar om hur JSON Web Tokens (JWT) utfärdas och valideras i backend-tjänsterna i NovaReport.

## 1. Miljövariabler

Alla tjänster delar samma JWT-relaterade konfiguration via miljövariabler:

- `JWT_SECRET` – symmetrisk nyckel som används för HS256-signering.
- `JWT_ISSUER` – förväntad issuer för alla access tokens (just nu `accounts-service`).
- `JWT_ACCESS_TOKEN_MINUTES` – livslängd för access tokens i minuter (t.ex. `30`).

Spring-properties:

- `jwt.secret = ${JWT_SECRET}`
- `jwt.issuer = ${JWT_ISSUER}`
- `jwt.access-token-minutes = ${JWT_ACCESS_TOKEN_MINUTES}`

## 2. Format på access tokens

Alla access tokens följer samma struktur:

- **Algoritm**: `HS256`
- **Issuer (`iss`)**: värdet från `JWT_ISSUER` (t.ex. `accounts-service`)
- **Subject (`sub`)**: användarens e-postadress
- **Claims**:
  - `uid` – användarens ID som UUID-sträng
  - `role` – användarroll, t.ex. `USER` eller `ADMIN`
- **Expiration (`exp`)**: `now + JWT_ACCESS_TOKEN_MINUTES`

Exempel på payload (endast illustrativt):

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

## 3. Authorization-header

Klienter och interna tjänster skickar access tokens i HTTP Authorization-headern:

- `Authorization: Bearer <access_token>`

Jag använder inga andra headers för JWT-autentisering.

## 4. Utfärda tokens

### Ansvar

- **accounts-service** är single source of truth för att utfärda access tokens till användare.
- Andra tjänster ska inte skapa egna användartokens i normala flöden.

### Implementation

I `accounts-service` använder jag `JwtService.createAccessToken(UUID userId, String email, String role)` med konventionerna ovan:

- `sub` = `email`
- `uid` = `userId.toString()`
- `role` = roll-strängen

Resultatet skickas tillbaka till frontenden vid lyckad login/registrering.

## 5. Validera tokens

Alla backend-tjänster validerar access tokens med samma regler:

1. **Signatur**
   - Verifiera HS256-signaturen med `JWT_SECRET`.
2. **Issuer**
   - Kräv att `iss == JWT_ISSUER`.
3. **Expiration**
   - Avvisa tokens där `exp` ligger i det förflutna.
4. **Claims**
   - Förvänta att `uid` och `role` finns när jag behöver användarkontext.

I praktiken gör jag detta via varje tjänsts `JwtService.parse(...)` eller motsvarande metod, som:

- Använder `jwt.secret` för att konfigurera signeringsnyckeln.
- Anropar `requireIssuer(jwt.issuer)` vid parsing.

`JwtService` i `payments-xmr-service` använder den nyare JJWT-API:n (`verifyWith(...).parseSignedClaims(...)`), men semantiken (nyckel + issuer + expiry) är densamma.

## 6. Använda claims i tjänsterna

När en tjänst behöver aktuellt user-id eller roll gör jag så här:

- Plockar ut token från `Authorization`-headern.
- Låter den lokala `JwtService` validera och parsa token.
- Läser claims från `Claims`-objektet, t.ex.:
  - `uid` → user-id (UUID som sträng)
  - `role` → användarroll

Ingen tjänst bör börja förlita sig på nya egna claims utan att jag uppdaterar det här dokumentet.

## 7. Framtida förbättringar

Om projektet skulle växa kan jag standardisera JWT-hanteringen ytterligare genom ett litet shared library, t.ex.:

- `JwtTokenFactory` – utfärdar tokens givet `userId`, `email`, `role`, `secret`, `issuer` och `ttl`.
- `JwtTokenParser` – validerar och parsar tokens och ser till att konventionerna i det här dokumentet följs.

För UMVP:en räcker det med konventionerna ovan, så länge jag håller `JwtService`-implementationerna i tjänsterna i linje med den här specen.
