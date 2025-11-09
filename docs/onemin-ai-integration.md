# 1min.ai Integration - COMPLETED ✅

**Datum:** 2025-11-09  
**Status:** ✅ KLAR OCH TESTAD  
**API-nyckel:** `b76c7d3cc16556797aeafebd8715aa1bddf5218a3082dde9afea871525e92f0d`

---

## 🚀 Snabbstart

### Aktivera AI-rapporter (3 steg)

1. **Starta tjänsten med AI aktiverad:**
   ```powershell
   cd deploy
   docker-compose up -d reporter-service
   ```
   *(API-nyckeln finns redan i `.env` filen)*

2. **Verifiera att AI är aktiverad:**
   ```powershell
   docker-compose logs reporter-service | Select-String "OneMinAi"
   ```
   Ska visa: `OneMinAiSummarizerService initialized with model: gpt-4o-mini`

3. **Testa AI-generering:**
   ```powershell
   cd ..
   .\test-onemin-ai.ps1
   ```

**Klart!** Rapporter genereras nu automatiskt var 4:e timme med riktig AI.

---

## Översikt

Integrationen med 1min.ai API ersätter fake AI-sammanfattningar med riktiga AI-genererade rapporter. Tjänsten använder GPT-modeller via 1min.ai för att skapa professionella, omfattande kryptorapporter.

**API-dokumentation:** https://docs.1min.ai/docs/api/ai-feature-api

---

## Vad som implementerats

### 1. OneMinAiSummarizerService
**Fil:** `service/OneMinAiSummarizerService.java`

**Funktioner:**
- ✅ Anropar 1min.ai Chat Completions API
- ✅ Använder GPT-4o-mini som standard (konfigurerbart)
- ✅ Professionell system-prompt för kryptoanalys
- ✅ Strukturerad rapportgenerering med sektioner
- ✅ Felhantering med fallback till manuell sammanfattning
- ✅ Conditional bean - aktiveras endast när `reporter.fake-ai=false`

**System Prompt:**
```
You are a professional cryptocurrency news analyst and report writer.

Guidelines:
- Professional, neutral, and informative tone
- Focus on significant developments and trends
- Organize information logically with clear sections
- Provide context and explain technical concepts
- Highlight market implications
- NO financial advice
- Factual reporting only
- Write in English

Structure:
1. Executive Summary (2-3 sentences overview)
2. Key Developments (main news with analysis)
3. Market Trends (patterns and themes)
4. Outlook (what to watch for)
```

**User Prompt:**
```
Create a comprehensive cryptocurrency market report for {date}.

Analyze and synthesize the following news items:
1. {headline 1}
2. {headline 2}
...

Requirements:
- Comprehensive analysis connecting related news
- Identify key trends and patterns
- Highlight significant developments
- Include context and implications
- 4-6 well-structured paragraphs (500-800 words)
- Clear headings for sections
- Professional, analytical tone
```

---

### 2. Konfiguration
**Fil:** `application.properties`

```properties
# 1min.ai configuration
onemin.api-key=${ONEMIN_API_KEY:}
onemin.model=${ONEMIN_MODEL:gpt-4o-mini}
```

**Tillgängliga modeller:**
- `gpt-4o-mini` (standard - snabb och kostnadseffektiv)
- `gpt-4o` (mer kraftfull)
- `gpt-3.5-turbo` (snabbast och billigast)

---

### 3. Docker Compose
**Fil:** `deploy/docker-compose.yml`

```yaml
reporter-service:
  environment:
    REPORTER_FAKE_AI: 'false'  # Aktivera riktig AI
    ONEMIN_API_KEY: ${ONEMIN_API_KEY:-}
    ONEMIN_MODEL: ${ONEMIN_MODEL:-gpt-4o-mini}
```

---

## Hur det fungerar

### Flöde

1. **RSS Ingest** (var 4:e timme)
   - Hämtar senaste nyheter från RSS-feeds
   - Deduplice ring (48h window)
   - Sparar nya nyheter i databasen

2. **Rapport-generering**
   - Hämtar top 10 senaste nyheter
   - Formaterar headlines med källa
   - Bygger prompt med system + user message

3. **1min.ai API-anrop**
   - POST till `https://api.1min.ai/v1/chat/completions`
   - Authorization: Bearer {API_KEY}
   - Model: gpt-4o-mini (default)
   - Temperature: 0.7
   - Max tokens: 2000

4. **Svar-hantering**
   - Extraherar AI-genererad text från response
   - Sparar rapport i databasen
   - Skickar notifiering till notifications-service

5. **Felhantering**
   - Vid API-fel: Loggar och skapar fallback-rapport
   - Fallback innehåller headlines + felmeddelande
   - Tjänsten fortsätter fungera även vid AI-fel

---

## Testning

Kör testskriptet:
```powershell
.\test-onemin-ai.ps1
```

Förväntat resultat: AI-genererad rapport på 500-800 ord visas.

---

## API-kostnader

**1min.ai priser (ungefärliga):**
- gpt-4o-mini: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- gpt-4o: ~$2.50 per 1M input tokens, ~$10 per 1M output tokens
- gpt-3.5-turbo: ~$0.50 per 1M input tokens, ~$1.50 per 1M output tokens

**Estimerad kostnad per rapport:**
- Input: ~500 tokens (headlines + prompt)
- Output: ~800 tokens (rapport)
- Kostnad med gpt-4o-mini: ~$0.0006 per rapport
- 6 rapporter/dag = ~$0.0036/dag = ~$0.11/månad

**Med gratis credits:**
- Typiskt 10,000-50,000 gratis tokens
- Räcker för 100-500 rapporter
- = 2-12 veckor med 6 rapporter/dag

---

## Produktionsinställningar

### Rekommenderad konfiguration

```yaml
# docker-compose.yml (produktion)
reporter-service:
  environment:
    REPORTER_FAKE_AI: 'false'
    ONEMIN_API_KEY: ${ONEMIN_API_KEY}  # Från secrets
    ONEMIN_MODEL: 'gpt-4o-mini'
    RSS_FEEDS: 'https://cointelegraph.com/rss,https://cryptonews.com/news/feed/'
```

### Säkerhetsrekommendationer

1. **API-nyckel:**
   - Använd Docker secrets eller Vault
   - Aldrig hårdkoda i kod eller config
   - Rotera regelbundet

2. **Rate limiting:**
   - 1min.ai har egna rate limits
   - Implementera retry-logik (redan finns)
   - Övervaka API-användning

3. **Monitoring:**
   - Logga API-fel
   - Alert vid upprepade fel
   - Övervaka kostnad/användning

---

## Felsökning

**Kolla loggar:**
```powershell
docker-compose logs reporter-service | Select-String "OneMin"
```

Ska visa: `OneMinAiSummarizerService initialized with model: gpt-4o-mini`

**Vanliga problem:**
- **401 Unauthorized:** Ogiltig API-nyckel
- **429 Too Many Requests:** Rate limit - vänta eller uppgradera
- **Fake AI används:** Kontrollera att `REPORTER_FAKE_AI=false` i `.env`

---

**Senast uppdaterad:** 2025-11-09
