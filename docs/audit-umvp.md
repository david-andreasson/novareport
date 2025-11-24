# Nova Report – Architecture & Code Audit

**Purpose:**  
Evaluate how well the Nova Report UMVP implementation follows its intended architecture and established software development best practices.

This document must be completed by Claude Sonnet 4.5.  
Claude should produce two separate reports:
1. **English version** – for professional review and when proposing changes.  
2. **Swedish version** – for internal reading and reflection.

Claude must not modify or generate code.  
The goal is analysis only.

---

## Instructions to Claude

### Context
- Repository root: `./`
- Project structure:
  - `/apps/accounts-service`
  - `/apps/subscriptions-service`
  - `/apps/reporter-service`
  - `/apps/notifications-service`
  - `/apps/payments-xmr-service`
  - `/apps/frontend`
  - `/deploy/docker-compose.yml`
  - `/docs`, `.gitattributes`, `.gitignore`, `README.md`
- Each service is a Spring Boot 3 application.
- Expected flow:
  `frontend → accounts-service → subscriptions-service → payments-xmr-service → reporter-service → notifications-service → user`
- Everything runs on a ubuntu server with Portainer for management.

---

## Task

Perform a **read-only technical audit** of the project.

Assess how well the code and structure align with:
- The intended architecture.
- Junior-level software engineering best practices.
- Clean Code and SOLID principles.

Do **not** change or refactor any code.
Provide two full reports:  
one in **English** and one in **Swedish**.

---

## Evaluation Criteria (Score 0–5)

| Area | Description |
|------|--------------|
| **1. Architecture Fit** | Clear boundaries, service coupling, adherence to intended flow. |
| **2. Clean Code & SOLID** | Naming, SRP, function size, duplication, clarity of intent. |
| **3. REST & Spring Conventions** | HTTP verbs, status codes, DTO usage, validation, `@ControllerAdvice`, ProblemDetails. |
| **4. Security & Config** | 12-Factor, env vars, secret handling, OWASP basics, validation. |
| **5. Testing** | Unit/controller test coverage, mocking, structure. |
| **6. Observability & Ops** | Actuator, logging, Dockerfile, Compose, health checks. |
| **7. Documentation & DX** | README completeness, clarity, English consistency. |

Scoring: 0 = missing, 3 = acceptable for MVP, 5 = excellent.

---

## Required Output Format

Claude must produce two full reports (English first, Swedish second), each containing the following sections:

1. **Executive Summary**  
   - 10 bullet points summarizing main strengths and weaknesses.

2. **Scores Table (0–5)**  
   - One table listing all seven evaluation areas with short comments.

3. **Architecture Fit**  
   - Describe how actual service interactions match or deviate from the target flow.  
   - Mention relevant files, classes, and boundaries.

4. **Clean Code & SOLID**  
   - List the top 10 findings (good or bad).  
   - For each: file + class + why it matters.

5. **REST & Spring Review**  
   - Table of real endpoints (path, verb, status codes, ProblemDetails yes/no).  
   - Comment on controller design and validation.

6. **Security & Config**  
   - Findings with file references and risk level (Low / Medium / High).  
   - Mention missing or exposed configurations.

7. **Testing Gaps**  
   - List missing tests with descriptive names and purpose (no code).

8. **Observability & Ops**  
   - Bullet list of existing vs missing elements for health, logs, Docker, Compose.

9. **Action Checklist**  
   - 10–15 short, prioritized steps to raise the project to solid junior-level quality.  
   - No code changes; plain text only.

---

## Standards to Check Against

- **Clean Code** (Robert C. Martin)  
- **SOLID principles**  
- **12-Factor App methodology**  
- **REST best practices & RFC 7807 Problem Details**  
- **Google Java Style / Checkstyle**  
- **OWASP Top 10** (security basics)  
- **SonarQube code smell rules**

---

## Constraints

- Output must be text-only — no diffs, code blocks, or patches.  
- Reference real file paths and classes where possible.  
- All code is assumed to be in English, but the report must appear in **both English and Swedish**.  
- Treat the report as a professional, structured code-review document.

---

Create a new file docs/audit-umvp-report-(todays date).md containing the full English and Swedish reports.


*End of instructions.*