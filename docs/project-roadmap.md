# Nova Report - Project Roadmap

**Skapad:** 2025-11-09  
**Senast uppdaterad:** 2025-11-21  
**Status:** Pågående (MVP-demo klar)  
**Mål:** Komplett fungerande mikroservice-arkitektur för AI-genererade kryptorapporter

---

## 📊 Nuläge - Vad är klart

### ✅ Färdiga tjänster
- **accounts-service** – Registrering, login, JWT ✅
- **subscriptions-service** – Prenumerationshantering ✅
- **payments-xmr-service** – Monero-betalningar via monero-wallet-rpc på stagenet ✅ (automatisk monitor + intern confirm-endpoint vid behov)
- **reporter-service** – RSS ingest, schemalagd generering var 4:e timme och 1min.ai-integration ✅
- **frontend** – Komplett MVP med prenumerationsflöde och rapportvy ✅

### ⚠️ Kvarstående kritiska brister
1. **Notifieringar i produktion** – Email/Discord finns implementerat men saknar ännu användarpreferenser, bättre felhantering och övervakning
2. **Monero mainnet & drift** – Betalningar körs på stagenet; switch till mainnet, fee/ekonomihantering och drifthärdning återstår
3. **Testtäckning ≈ 0%** – Kräver automatiska tester och regressioner
4. **Secrets & säkerhet** – API-nycklar och interna nycklar ligger i miljövariabler/konfig
5. **Observability** – Ingen central övervakning, loggaggregat eller larm

---

## 🎯 PRIO 1: Gör systemet användbart (2-3 veckor)

### 1.1 Frontend: Prenumerationsflöde ✅ KLAR (2025-11-09)
**Vad som levererats:**
- Ny `Prenumerera`-vy med planval (månad/år)
- Integration mot `POST /api/v1/payments/create` och polling av status
- UI för Monero-adress, kopieringsknappar och testinstruktioner
- CORS-stöd i payments-service + hjälpskript (`confirm-payment.ps1`, `test-payment-flow.ps1`)

**Kända förbättringar framåt:**
- Generera riktig QR-kod (placeholder idag)
- Visa betalningshistorik
- Låsa knapp efter bekräftad prenumeration

---

### 1.2 Reporter: Scheduler för automatisk rapportgenerering ✅ KLAR
**Varför:** Rapporter genereras inte automatiskt - måste köras manuellt!  
**Status:** ✅ **IMPLEMENTERAD OCH TESTAD** (2025-11-09)

**Schema:** Rapporter ska genereras **var 4:e timme** (6 rapporter per dag)
- 00:00, 04:00, 08:00, 12:00, 16:00, 20:00

**Implementering:**

**Steg 1: Aktivera scheduling**
```java
// ReporterServiceApplication.java
@SpringBootApplication
@EnableScheduling  // ← Lägg till detta
@EnableConfigurationProperties(ReporterProperties.class)
public class ReporterServiceApplication {
    // ...
}
```

**Steg 2: Skapa scheduled job**
```java
// service/ScheduledReportGenerator.java
package com.novareport.reporter_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ScheduledReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportGenerator.class);

    private final ReporterCoordinator coordinator;

    public ScheduledReportGenerator(ReporterCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Generates reports every 4 hours: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
     */
    @Scheduled(cron = "0 0 0/4 * * *")
    public void generateReport() {
        log.info("Starting scheduled report generation");
        try {
            // 1. Ingest latest RSS feeds
            var ingestResult = coordinator.ingestNow();
            log.info("Ingested {} new items, {} duplicates", 
                ingestResult.newItems(), ingestResult.duplicates());

            // 2. Build report for today
            LocalDate today = LocalDate.now();
            var report = coordinator.buildReport(today);
            log.info("Generated report for {} with {} chars", 
                today, report.getSummary().length());

        } catch (Exception e) {
            log.error("Failed to generate scheduled report", e);
        }
    }
}
```

**Steg 3: Konfigurera thread pool**
```properties
# application-dev.properties
spring.task.scheduling.pool.size=2
spring.task.scheduling.thread-name-prefix=reporter-scheduler-
```

**Testning:**
```bash
# Manuellt trigger via internt API
curl -X POST http://localhost:8082/api/v1/internal/reporter/ingest \
  -H "X-INTERNAL-KEY: dev-change-me"

curl -X POST http://localhost:8082/api/v1/internal/reporter/build-report \
  -H "X-INTERNAL-KEY: dev-change-me" \
  -H "Content-Type: application/json" \
  -d '{"date": "2024-11-09"}'
```

**Estimerad tid:** 2-3 timmar

---

### 1.3 Reporter: Integrera 1min.ai för riktig AI-generering ✅ KLAR
**Varför:** Fake AI ger ingen värde - behöver riktigt innehåll!  
**Status:** ✅ **IMPLEMENTERAD OCH TESTAD** (2025-11-09)

**API:** https://docs.1min.ai/docs/api/ai-feature-api

**Implementering:**

**Steg 1: Lägg till dependencies**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**Steg 2: Skapa 1min.ai client**
```java
// service/OneMinAiSummaryService.java
package com.novareport.reporter_service.service;

import com.novareport.reporter_service.domain.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "reporter.fake-ai", havingValue = "false")
public class OneMinAiSummaryService implements AiSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(OneMinAiSummaryService.class);
    private static final String API_URL = "https://api.1min.ai/v1/chat/completions";

    private final WebClient webClient;
    private final String apiKey;

    public OneMinAiSummaryService(
            WebClient.Builder webClientBuilder,
            @Value("${onemin.api-key}") String apiKey
    ) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
        this.apiKey = apiKey;
    }

    @Override
    public String summarize(List<NewsItem> items) {
        if (items.isEmpty()) {
            return "No news items to summarize.";
        }

        String newsContent = items.stream()
                .map(item -> String.format("- %s: %s", item.getTitle(), item.getDescription()))
                .collect(Collectors.joining("\n"));

        String prompt = buildPrompt(newsContent);

        try {
            Map<String, Object> request = Map.of(
                    "model", "gpt-4o-mini",  // eller "gpt-3.5-turbo" för snabbare/billigare
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a professional crypto news analyst."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            Map<String, Object> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String summary = extractSummary(response);
            log.info("Generated AI summary with {} characters", summary.length());
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate AI summary", e);
            return "Failed to generate summary: " + e.getMessage();
        }
    }

    private String buildPrompt(String newsContent) {
        return String.format("""
                Analyze the following cryptocurrency news and create a concise, neutral summary.
                Focus on the most important developments and trends.
                
                News items:
                %s
                
                Provide a summary in 3-5 paragraphs that:
                1. Highlights the most significant events
                2. Identifies key trends or patterns
                3. Maintains a neutral, informative tone
                4. Is suitable for investors and crypto enthusiasts
                
                Do not provide financial advice. Stick to factual reporting.
                """, newsContent);
    }

    private String extractSummary(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No choices in API response");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
```

**Steg 3: Konfigurera**
```properties
# application-dev.properties
reporter.fake-ai=false
onemin.api-key=${ONEMIN_API_KEY:your-api-key-here}
```

**Steg 4: Uppdatera docker-compose.yml**
```yaml
reporter-service:
  environment:
    REPORTER_FAKE_AI: 'false'
    ONEMIN_API_KEY: ${ONEMIN_API_KEY}
```

**Testning:**
```bash
# Sätt API key
export ONEMIN_API_KEY=your-actual-key

# Starta tjänsten
cd apps/reporter-service
./mvnw spring-boot:run

# Trigger rapport-generering
curl -X POST http://localhost:8082/api/v1/internal/reporter/build-report \
  -H "X-INTERNAL-KEY: dev-change-me" \
  -H "Content-Type: application/json" \
  -d '{"date": "2024-11-09"}'
```

**Estimerad tid:** 6-8 timmar

---

## 🎯 PRIO 2: Gör notifieringar riktiga (1 vecka)

**Status:** Genomfört – email- och Discord-notiser är implementerade i `notifications-service`. Nedan beskrivs den ursprungliga designskissen (kan användas som referens, men koden följer inte exakt dessa exempel).

### 2.1 Email-integration
**Vad som behövs:**
- [ ] Spring Mail dependency
- [ ] SMTP-konfiguration (Gmail/SendGrid)
- [ ] HTML email template
- [ ] EmailNotificationService
- [ ] User email preferences i accounts-service

**Implementering:**
```java
@Service
public class EmailNotificationService {
    private final JavaMailSender mailSender;
    
    public void sendReportEmail(String to, String reportDate, String summary) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject("Nova Report - " + reportDate);
        helper.setText(buildHtmlEmail(reportDate, summary), true);
        
        mailSender.send(message);
    }
}
```

**Estimerad tid:** 4-6 timmar

---

### 2.2 Discord-integration
**Vad som behövs:**
- [x] Discord webhook URL
- [x] DiscordNotificationService
- [x] Format rapport som Discord embed

**Implementering:**
```java
@Service
public class DiscordNotificationService {
    private final WebClient webClient;
    
    public void sendReportToDiscord(String reportDate, String summary) {
        Map<String, Object> embed = Map.of(
            "title", "Nova Report - " + reportDate,
            "description", summary.substring(0, Math.min(2000, summary.length())),
            "color", 0x00ff00
        );
        
        webClient.post()
            .uri(webhookUrl)
            .bodyValue(Map.of("embeds", List.of(embed)))
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }
}
```

**Estimerad tid:** 2-3 timmar

---

## PRIO 3: Lägg till tester (2 veckor)

### Tester per tjänst

**accounts-service:**
- [ ] User registration tests
- [ ] Login tests
- [ ] JWT validation tests
- [ ] Settings update tests

**subscriptions-service:**
- [ ] Activate subscription tests
- [ ] Has-access tests
- [ ] Expiry tests

**payments-xmr-service:**
- [ ] Create payment tests
- [ ] Confirm payment tests
- [ ] Integration tests med subscriptions-service
- [ ] Security tests (SSRF, race conditions)

**reporter-service:**
- [ ] RSS ingest tests
- [ ] Deduplication tests
- [ ] AI summarization tests (mock)
- [ ] Scheduler tests

**notifications-service:**
- [x] Email sending tests
- [x] Discord webhook tests

**Estimerad tid:** 40-60 timmar totalt

---

## PRIO 4: Produktionsgör (2-3 veckor)

### 4.1 Monero i produktion
- [x] Integrera monero-wallet-rpc (stagenet)
- [x] Automatisk betalningsövervakning (PaymentMonitorService)
- [ ] Docker setup/dokumentation för monerod + wallet i produktion/mainnet

**Estimerad tid:** 8-12 timmar

---

### 4.2 Säkerhet
- [ ] Secrets management (Docker secrets/Vault)
- [ ] HTTPS/TLS (Nginx + Let's Encrypt)
- [ ] Rate limiting
- [ ] Security headers

**Estimerad tid:** 8-10 timmar

---

### 4.3 Monitoring
- [ ] Prometheus metrics
- [ ] Grafana dashboards
- [ ] Alerting (betalningar fastnar, rapporter misslyckas)
- [ ] Logging aggregation

**Estimerad tid:** 8-10 timmar

---

### 4.4 Databas
- [ ] Migrera från H2 till PostgreSQL
- [ ] Backup strategy
- [ ] Connection pooling

**Estimerad tid:** 6-8 timmar

---

## Tidslinje

### Vecka 1-2: MVP - Gör det användbart
- [x] Frontend betalningssida (4-6h) 
- [x] Reporter scheduler (2-3h) 
- [x] 1min.ai integration (6-8h) 
- [x] Manuell testning av hela flödet (4h) 

**Mål:** Användare kan köpa prenumeration och få AI-genererade rapporter var 4:e timme

---

### Vecka 3-4: Gör det komplett
- [x] Email notiser (4-6h)
- [x] Discord notiser (2-3h)
- [ ] Tester för alla tjänster (40-60h)
- [ ] Subscription management (cancel, renew) (8h)

**Mål:** Komplett funktionalitet med notiser och tester

---

### Vecka 5-7: Produktionsgör
- [ ] Monero mainnet & driftsättning (8-12h)
- [ ] Säkerhet (8-10h)
- [ ] Monitoring (8-10h)
- [ ] PostgreSQL (6-8h)
- [ ] Deploy till produktion (8h)

**Mål:** Production-ready system

---

## 🚀 Nästa Steg

**Börja med dessa 3 i ordning:**

1. **Notifieringar (email + Discord)** – ger användare värde direkt
2. **Testtäckning & automatiserade tester** – hindra regressioner
3. **Riktig Monero-backend** – ta steget från demo till betalprodukt

**Dessa tre lyfter MVP-demo till ett produktionsdugligt system.** 🎉

---

## 📝 Anteckningar

### 1min.ai API
- **Dokumentation:** https://docs.1min.ai/docs/api/ai-feature-api
- **Modeller:** gpt-4o-mini, gpt-3.5-turbo, gpt-4
- **Rate limits:** Kolla dokumentationen
- **Kostnad:** Gratis credits tillgängliga

### Rapportschema
- **Frekvens:** Var 4:e timme (6 rapporter/dag)
- **Tider:** 00:00, 04:00, 08:00, 12:00, 16:00, 20:00
- **Timezone:** Europe/Stockholm (konfigurerbart)

### Teknisk skuld att adressera
- Inga tester i någon tjänst
- Hårdkodade secrets överallt
- H2-databas (inte production-ready)
- Monero körs på stagenet (mainnet-setup och ekonomi återstår)
- Ingen central monitoring
- Ingen backup-strategi

---

