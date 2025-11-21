# NovaReport Frontend

React/TypeScript-frontend för NovaReport-plattformen. Den här appen hanterar:

- registrering och login
- prenumerationsflöde med Monero-betalning
- visning av senaste AI-genererade kryptorapport

Frontenden bygger ovanpå **Vite + React + TypeScript** men är anpassad specifikt för NovaReport-backenden.

---

## Funktioner

- Login/registrering mot `accounts-service`
- Köpa prenumeration och skapa betalning via `payments-xmr-service`
- Kontrollera prenumerationsstatus via `subscriptions-service`
- Hämta och visa senaste rapporten från `reporter-service`/`notifications-service`
- Visa rapportens sammanfattning med enkel markdown-liknande formattering (rubriker, fetstil)

---

## Utvecklingsmiljö – hur jag körde frontenden lokalt i början

Jag har kört väldigt lite ren frontend-utveckling lokalt. Oftast har jag kört hela systemet via Docker på min Ubuntu-server, men i början av projektet startade jag frontenden några gånger lokalt för att testa.

### Förutsättningar (när jag kör lokalt)

- Node.js (LTS)
- npm eller yarn

### Så här körde jag `npm run dev` i början

```bash
cd apps/frontend
npm install
npm run dev
```

När jag kör den så här brukar Vite ligga på `http://localhost:5173`.

För att frontenden ska fungera fullt ut behöver backend-tjänsterna också vara igång. När jag ville spegla den riktiga miljön använde jag `deploy/docker-compose.yml`:

```bash
cd deploy
docker-compose up --build
```

I Compose-filen har jag konfigurerat frontend-bilden med följande build-args (motsvarar `VITE_*`-variabler i Vite):

- `VITE_ACCOUNTS_API_BASE` – bas-URL till accounts-service (t.ex. `http://localhost:8080`)
- `VITE_SUBS_API_BASE` – bas-URL till subscriptions-service
- `VITE_NOTIF_API_BASE` – bas-URL till notifications-service
- `VITE_REPORTER_API_BASE` – bas-URL till reporter-service
- `VITE_PAYMENTS_API_BASE` – bas-URL till payments-xmr-service
- `VITE_INTERNAL_API_KEY` – intern nyckel som skickas till interna endpoints där det krävs

Om jag någon gång vill köra frontenden lokalt utan Docker kan jag sätta motsvarande variabler i en `.env`-fil i `apps/frontend/`:

```env
VITE_ACCOUNTS_API_BASE=http://localhost:8080
VITE_SUBS_API_BASE=http://localhost:8081
VITE_NOTIF_API_BASE=http://localhost:8083
VITE_REPORTER_API_BASE=http://localhost:8082
VITE_PAYMENTS_API_BASE=http://localhost:8084
VITE_INTERNAL_API_KEY=dev-change-me
```

Sedan räcker det att köra `npm run dev` för att frontenden ska använda dessa värden.

---

## Bygga för produktion

I normalfallet byggs frontenden som en Docker-image (se `docker-compose.prod.yml`). Om du vill bygga lokalt:

```bash
cd apps/frontend
npm install
npm run build
```

Detta genererar en statisk build i `dist/` som kan serveras av valfri HTTP-server.

---

## Lint & kodkvalitet

Nyttiga scripts:

- `npm run lint` – kör ESLint
- `npm run build` – typecheck + build

Se även `README-CODE-QUALITY.md` i projektroten för generella riktlinjer kring kodkvalitet.
