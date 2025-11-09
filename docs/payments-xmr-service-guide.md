# Guide: Bygg payments-xmr-service (Enkel version)

**Datum:** 2024-11-06  
**Syfte:** Skapa en fungerande betalningsservice för Monero (XMR) som aktiverar prenumerationer

---

## Översikt – Vad ska tjänsten göra?

`payments-xmr-service` är den tjänst som tar emot betalningar i Monero (XMR) och aktiverar användarens prenumeration när betalningen är bekräftad.

### Flödet i projektet (hur det ska fungera)

1. **Användaren** loggar in via `accounts-service` och får en JWT-token
2. **Användaren** vill köpa en prenumeration och klickar på "Betala med Monero"
3. **Frontend** anropar `payments-xmr-service` och begär en betalningsadress
4. **payments-xmr-service** skapar en unik Monero-adress för betalningen
5. **Användaren** skickar XMR till den adressen från sin plånbok
6. **payments-xmr-service** lyssnar efter inkommande betalningar
7. När betalningen är bekräftad → **payments-xmr-service** anropar `subscriptions-service` och aktiverar prenumerationen
8. **subscriptions-service** sparar att användaren nu har en aktiv prenumeration
9. **Användaren** kan nu använda `reporter-service` för att läsa rapporter

---

## Enkel version – Vad vi bygger först

För att göra det enkelt i första skedet ska vi **INTE** integrera med riktiga Monero-noder eller plånböcker. Istället bygger vi en **simulerad** betalningsservice som:

- Skapar "fake" betalningsadresser
- Låter dig manuellt markera en betalning som "betald" via ett internt API
- Aktiverar prenumerationen när betalningen är markerad som betald

Detta gör att du kan testa hela flödet utan att behöva sätta upp Monero-infrastruktur.

---

## Vad tjänsten behöver innehålla

### 1. Databas (H2 för utveckling)

Tjänsten behöver spara information om betalningar. Vi behöver en tabell som heter `payments` med följande kolumner:

- `id` – Unikt ID för betalningen (UUID)
- `user_id` – Vilket användar-ID som ska få prenumerationen (UUID)
- `payment_address` – Den Monero-adress som genererades (text)
- `amount_xmr` – Hur mycket XMR som ska betalas (decimaltal)
- `plan` – Vilken prenumerationsplan (t.ex. "monthly", "yearly")
- `duration_days` – Hur många dagar prenumerationen ska vara aktiv (heltal)
- `status` – Status på betalningen: PENDING, CONFIRMED, FAILED (text)
- `created_at` – När betalningen skapades (tidsstämpel)
- `confirmed_at` – När betalningen bekräftades (tidsstämpel, kan vara null)

### 2. REST API endpoints

Tjänsten behöver tre endpoints:

#### A. Skapa en betalning (publik endpoint)
- **Sökväg:** `POST /api/v1/payments/create`
- **Vem anropar:** Frontend (användaren som är inloggad)
- **Input:** JWT-token (för att veta vem användaren är), plan (t.ex. "monthly"), amount
- **Output:** En betalningsadress och betalnings-ID
- **Vad händer:** Tjänsten skapar en ny rad i databasen med status PENDING och genererar en fake Monero-adress

#### B. Kontrollera betalningsstatus (publik endpoint)
- **Sökväg:** `GET /api/v1/payments/{paymentId}/status`
- **Vem anropar:** Frontend (för att visa användaren om betalningen är klar)
- **Input:** Betalnings-ID
- **Output:** Status (PENDING, CONFIRMED, FAILED)
- **Vad händer:** Tjänsten kollar i databasen och returnerar aktuell status

#### C. Bekräfta betalning manuellt (internt endpoint)
- **Sökväg:** `POST /api/v1/internal/payments/{paymentId}/confirm`
- **Vem anropar:** Du själv (för testning) eller en admin
- **Input:** Betalnings-ID och intern API-nyckel
- **Output:** Bekräftelse att betalningen är godkänd
- **Vad händer:** 
  1. Tjänsten uppdaterar status till CONFIRMED i databasen
  2. Tjänsten anropar `subscriptions-service` på `/api/v1/internal/subscriptions/activate`
  3. Prenumerationen aktiveras för användaren

### 3. Integration med subscriptions-service

När en betalning bekräftas måste `payments-xmr-service` prata med `subscriptions-service`. Detta görs via ett HTTP-anrop (WebClient i Spring).

**Anropet ser ut så här:**
- **URL:** `http://subscriptions-service:8080/api/v1/internal/subscriptions/activate`
- **Metod:** POST
- **Headers:** `X-INTERNAL-KEY: dev-change-me` (den interna API-nyckeln)
- **Body (JSON):**
```json
{
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "plan": "monthly",
  "durationDays": 30
}
```

När `subscriptions-service` tar emot detta anrop skapar den en aktiv prenumeration för användaren.

---

## Filstruktur för payments-xmr-service

Här är vilka filer du behöver skapa:

```
apps/payments-xmr-service/
├── src/main/java/com/novareport/payments_xmr_service/
│   ├── PaymentsXmrServiceApplication.java (finns redan)
│   ├── domain/
│   │   ├── Payment.java (entitet för databasen)
│   │   ├── PaymentStatus.java (enum: PENDING, CONFIRMED, FAILED)
│   │   └── PaymentRepository.java (JPA repository)
│   ├── dto/
│   │   ├── CreatePaymentRequest.java (input för att skapa betalning)
│   │   ├── CreatePaymentResponse.java (output med adress och ID)
│   │   ├── PaymentStatusResponse.java (output för status)
│   │   └── ActivateSubscriptionRequest.java (för anrop till subscriptions-service)
│   ├── service/
│   │   ├── PaymentService.java (affärslogik för betalningar)
│   │   └── SubscriptionsClient.java (WebClient för att prata med subscriptions-service)
│   ├── controller/
│   │   ├── PaymentController.java (publika endpoints)
│   │   └── InternalPaymentController.java (interna endpoints)
│   └── config/
│       ├── SecurityConfig.java (säkerhetsinställningar)
│       └── WebClientConfig.java (konfiguration för HTTP-anrop)
└── src/main/resources/
    ├── application.properties
    ├── application-dev.properties
    └── db/migration/
        └── V1__create_payments_table.sql (Flyway-migration för databasen)
```

---

## Steg-för-steg: Vad du ska göra

### Steg 1: Skapa databasmodellen

**Fil:** `Payment.java`

Detta är en JPA-entitet som representerar en rad i `payments`-tabellen.

**Vad den innehåller:**
- Alla kolumner som beskrivs ovan (id, user_id, payment_address, osv.)
- Annotationer som `@Entity`, `@Id`, `@Column`
- Getters och setters (använd Lombok för att slippa skriva dessa)

**Fil:** `PaymentStatus.java`

En enkel enum med tre värden: `PENDING`, `CONFIRMED`, `FAILED`

**Fil:** `PaymentRepository.java`

Ett interface som ärver från `JpaRepository<Payment, UUID>`. Spring skapar automatiskt metoderna åt dig.

Du behöver lägga till en metod:
```java
Optional<Payment> findByIdAndUserId(UUID id, UUID userId);
```

### Steg 2: Skapa DTO:er (Data Transfer Objects)

Dessa är enkla klasser som används för att skicka data in och ut från API:et.

**CreatePaymentRequest:**
- `plan` (String) – t.ex. "monthly"
- `amountXmr` (BigDecimal) – t.ex. 0.05

**CreatePaymentResponse:**
- `paymentId` (UUID)
- `paymentAddress` (String) – den genererade Monero-adressen
- `amountXmr` (BigDecimal)
- `expiresAt` (Instant) – när betalningen går ut (t.ex. 24 timmar)

**PaymentStatusResponse:**
- `paymentId` (UUID)
- `status` (PaymentStatus)
- `createdAt` (Instant)
- `confirmedAt` (Instant, kan vara null)

**ActivateSubscriptionRequest:**
- `userId` (UUID)
- `plan` (String)
- `durationDays` (int)

### Steg 3: Skapa PaymentService

Detta är hjärtat i tjänsten. Här ligger all affärslogik.

**Metoder som behövs:**

**`createPayment(UUID userId, String plan, BigDecimal amountXmr)`**
- Skapar en ny Payment-entitet
- Genererar en fake Monero-adress (t.ex. "XMR-" + UUID.randomUUID())
- Sätter status till PENDING
- Sätter duration_days baserat på plan (monthly = 30, yearly = 365)
- Sparar i databasen
- Returnerar CreatePaymentResponse

**`getPaymentStatus(UUID paymentId, UUID userId)`**
- Hämtar betalningen från databasen
- Kontrollerar att den tillhör rätt användare
- Returnerar PaymentStatusResponse

**`confirmPayment(UUID paymentId)`**
- Hämtar betalningen från databasen
- Kontrollerar att status är PENDING
- Uppdaterar status till CONFIRMED
- Sätter confirmed_at till nu
- Sparar i databasen
- Anropar SubscriptionsClient för att aktivera prenumerationen
- Returnerar bekräftelse

### Steg 4: Skapa SubscriptionsClient

Denna klass använder WebClient för att göra HTTP-anrop till subscriptions-service.

**Metod som behövs:**

**`activateSubscription(UUID userId, String plan, int durationDays)`**
- Skapar en ActivateSubscriptionRequest
- Gör ett POST-anrop till `http://subscriptions-service:8080/api/v1/internal/subscriptions/activate`
- Lägger till header `X-INTERNAL-KEY` med värdet från konfigurationen
- Skickar JSON-body
- Returnerar void (eller kastar exception om det misslyckas)

### Steg 5: Skapa Controllers

**PaymentController (publika endpoints):**

**`POST /api/v1/payments/create`**
- Läser JWT-token från Authorization-headern
- Extraherar userId från token
- Validerar input (CreatePaymentRequest)
- Anropar PaymentService.createPayment()
- Returnerar CreatePaymentResponse

**`GET /api/v1/payments/{paymentId}/status`**
- Läser JWT-token från Authorization-headern
- Extraherar userId från token
- Anropar PaymentService.getPaymentStatus()
- Returnerar PaymentStatusResponse

**InternalPaymentController (interna endpoints):**

**`POST /api/v1/internal/payments/{paymentId}/confirm`**
- Kontrollerar att X-INTERNAL-KEY är korrekt
- Anropar PaymentService.confirmPayment()
- Returnerar 202 Accepted

### Steg 6: Konfigurera säkerhet

**SecurityConfig:**
- Tillåt `/api/v1/payments/**` för autentiserade användare (JWT)
- Tillåt `/api/v1/internal/**` utan JWT men kräv X-INTERNAL-KEY
- Stäng av CSRF (eftersom det är ett API)

**WebClientConfig:**
- Skapa en `@Bean` för WebClient
- Används av SubscriptionsClient

### Steg 7: Konfigurera application-dev.properties

Lägg till:
```properties
server.port=8084

spring.datasource.url=jdbc:h2:file:${user.home}/.novareport/payments-xmr-service;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

jwt.issuer=accounts-service
jwt.secret=dev-secret-change-me-please-32-bytes-minimum

internal.api-key=dev-change-me

subscriptions.base-url=http://localhost:8081
```

### Steg 8: Skapa Flyway-migration

**Fil:** `V1__create_payments_table.sql`

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    payment_address VARCHAR(255) NOT NULL,
    amount_xmr DECIMAL(19, 8) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    duration_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
```

### Steg 9: Uppdatera docker-compose.yml

Lägg till payments-xmr-service i `deploy/docker-compose.yml`:

```yaml
payments-xmr-service:
  build:
    context: ../apps/payments-xmr-service
  image: novareport/payments-xmr-service:dev
  environment:
    SPRING_PROFILES_ACTIVE: dev
    JWT_SECRET: dev-secret-change-me-please-32-bytes-minimum
    JWT_ISSUER: accounts-service
    INTERNAL_API_KEY: dev-change-me
    SUBSCRIPTIONS_BASE_URL: http://subscriptions-service:8080
  ports:
    - '8084:8084'
  networks:
    - novareport
  depends_on:
    - subscriptions-service
```

---

## Hur du testar flödet

### 1. Starta alla tjänster
```bash
cd deploy
docker-compose up --build
```

### 2. Registrera en användare
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

Du får tillbaka en JWT-token. Spara den.

### 3. Skapa en betalning
```bash
curl -X POST http://localhost:8084/api/v1/payments/create \
  -H "Authorization: Bearer DIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "plan": "monthly",
    "amountXmr": 0.05
  }'
```

Du får tillbaka ett paymentId och en paymentAddress.

### 4. Bekräfta betalningen manuellt (simulera att användaren betalat)
```bash
curl -X POST http://localhost:8084/api/v1/internal/payments/PAYMENT_ID/confirm \
  -H "X-INTERNAL-KEY: dev-change-me"
```

### 5. Kontrollera att prenumerationen är aktiv
```bash
curl -X GET http://localhost:8081/api/v1/subscriptions/me/has-access \
  -H "Authorization: Bearer DIN_JWT_TOKEN"
```

Du ska få tillbaka `{"hasAccess": true}`

### 6. Testa att hämta en rapport
```bash
curl -X GET http://localhost:8082/api/v1/reports/latest \
  -H "Authorization: Bearer DIN_JWT_TOKEN"
```

Nu ska du kunna se rapporter eftersom du har en aktiv prenumeration!

---

## Sammanfattning

**Vad vi byggt:**
- En betalningsservice som skapar fake Monero-adresser
- Ett sätt att manuellt bekräfta betalningar
- Integration med subscriptions-service för att aktivera prenumerationer
- Komplett flöde från betalning till aktiv prenumeration

**Vad som INTE ingår än:**
- Riktig Monero-integration (monero-wallet-rpc)
- Automatisk betalningsövervakning
- Återbetalningar
- Betalningshistorik för användaren

**Nästa steg (framtida förbättringar):**
1. Integrera med monero-wallet-rpc för riktiga betalningar
2. Lägg till en bakgrundsjobb som kollar efter inkommande betalningar
3. Lägg till endpoint för att lista användarens betalningar
4. Lägg till webhook för att notifiera frontend när betalning är klar
5. Lägg till tester för PaymentService och controllers

---

**Lycka till med implementationen!** 🚀
