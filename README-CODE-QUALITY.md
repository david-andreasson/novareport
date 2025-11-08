# Code Quality Guide

## Innan varje commit

Kör detta script för att undvika Copilot-kommentarer:

```powershell
.\check-code.ps1
```

Detta kör:
1. **Kompilering** - Hittar syntax-fel och type errors
2. **Tester** - Verifierar att all funktionalitet fungerar
3. **Package** - Säkerställer att JAR kan byggas

## Snabb-check för en specifik service

```powershell
cd apps/payments-xmr-service
./mvnw clean verify
```

## Vad Copilot kollar efter

Copilot's AI-review letar efter:

### 🔴 Kritiska problem
- **Security issues**: Timing attacks, hard-coded secrets, SQL injection
- **Concurrency bugs**: Race conditions, deadlocks, improper locking
- **Resource leaks**: Unclosed connections, memory leaks
- **Data integrity**: Transaction boundaries, isolation levels

### 🟡 Code quality
- **Performance**: Inefficient queries, N+1 problems, blocking calls
- **Maintainability**: Code duplication, magic numbers, poor naming
- **Best practices**: Proper error handling, logging, validation
- **Architecture**: Separation of concerns, dependency management

### 🟢 Style & conventions
- **Imports**: Unused imports, wrong package structure
- **Documentation**: Missing Javadoc, unclear comments
- **Formatting**: Inconsistent style, long methods

## Automatisk check vid PR

GitHub Actions kör automatiskt samma checks när du skapar en PR.
Se `.github/workflows/code-quality.yml` för detaljer.

## Tips för att undvika vanliga problem

### 1. Transaction management
```java
// ✗ Fel: Blocking call i transaction
@Transactional
public void process() {
    // ... database operations ...
    externalService.call().block(); // Blocking!
}

// ✓ Rätt: Använd events eller separata transactions
@Transactional
public void process() {
    // ... database operations ...
    eventPublisher.publish(new ProcessedEvent());
}
```

### 2. Resource management
```java
// ✗ Fel: RestTemplate/WebClient utan timeout
WebClient.create().get().retrieve().block();

// ✓ Rätt: Konfigurera timeouts
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .build();
}
```

### 3. Security
```java
// ✗ Fel: String comparison (timing attack)
if (apiKey.equals(expectedKey)) { ... }

// ✓ Rätt: Constant-time comparison
if (MessageDigest.isEqual(
    apiKey.getBytes(UTF_8),
    expectedKey.getBytes(UTF_8))) { ... }
```

### 4. Configuration
```java
// ✗ Fel: Hard-coded values
server.port=8080

// ✓ Rätt: Environment variables med defaults
server.port=${SERVER_PORT:8080}
```

## Lägg till fler checks (valfritt)

För ännu bättre kodkvalitet, lägg till i `pom.xml`:

```xml
<!-- SpotBugs - hittar buggar -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.0</version>
</plugin>

<!-- Checkstyle - kodstil -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
</plugin>

<!-- PMD - code smells -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.21.2</version>
</plugin>
```

Kör sedan:
```powershell
./mvnw spotbugs:check checkstyle:check pmd:check
```
