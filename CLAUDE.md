# CLAUDE.md

## Project Overview

**PocUnblu** is a Proof of Concept demonstrating integration of [Unblu](https://www.unblu.com/) (a real-time
communication platform) with enterprise systems using **Hexagonal Architecture** and **Apache Camel** for intelligent
conversation orchestration.

## Architecture

The project follows **Hexagonal Architecture (Ports & Adapters)** with a **Maven multi-module** structure:

```
unblu-domain          → Core business logic, domain models, port interfaces (zero external deps)
unblu-application     → Use case implementation, Apache Camel orchestration routes
unblu-infrastructure  → Technical adapters (Unblu API, ERP mock, rule engine mock, summary)
unblu-exposition      → REST API layer (primary adapters via Camel REST DSL)
unblu-configuration   → Spring Boot entry point, dependency assembly, frontend serving
unblu-frontend        → Angular 20 standalone components (testing UI)
```

**Data flow:** REST Request → Exposition → Application (Camel routes) → Domain Ports → Infrastructure Adapters → Unblu
API

## Tech Stack

| Layer       | Technology                                   |
|-------------|----------------------------------------------|
| Language    | Java 21                                      |
| Framework   | Spring Boot 3.5.11                           |
| Integration | Apache Camel 4.18.0                          |
| Unblu SDK   | 8.32.3 (jersey3-client-v4)                   |
| Resilience  | Resilience4j (via Camel)                     |
| Frontend    | Angular 20.3.0 (standalone)                  |
| Build       | Maven (multi-module) + frontend-maven-plugin |

## Build & Run

### Full Build (Backend + Frontend)

```bash
mvn clean install
```

### Run Application

```bash
mvn spring-boot:run -pl unblu-configuration
```

**With Debug Profile (Verbose logs & SQL):**

```bash
mvn spring-boot:run -pl unblu-configuration -Dspring-boot.run.profiles=debug
```

- App runs on **port 8081**
- Angular UI at `http://localhost:8081/`
- REST API at `http://localhost:8081/api/v1/`
- Swagger UI at `http://localhost:8081/swagger-ui/`

### Frontend Only

```bash
./build-frontend.sh
# or manually:
cd unblu-frontend && npm install && npm run build
```

### Frontend Dev Server

```bash
cd unblu-frontend && npm start   # proxies API to localhost:8081
```

## Configuration

### Required Environment Variables

```bash
UNBLU_API_USERNAME=<your-unblu-username>
UNBLU_API_PASSWORD=<your-unblu-password>
UNBLU_API_BASE_URL=https://services8.unblu.com/app/rest/v4  # optional, has default
```

Use a `.env` file for local development (never commit it — it's in `.gitignore`):

```bash
cp .env.example .env
# then fill in your credentials
```

### Key Config Files

- `unblu-configuration/src/main/resources/application.properties` — server port, static resources, logging
- `unblu-application/src/main/resources/application.yaml` — Unblu API base URL, proxy settings
- `unblu-frontend/proxy.conf.json` — Angular dev proxy to backend

## REST API Endpoints

| Method | Path                           | Description                       |
|--------|--------------------------------|-----------------------------------|
| POST   | `/api/v1/conversations/start`  | Create conversation with a team   |
| POST   | `/api/v1/conversations/direct` | Create direct 1-to-1 conversation |
| GET    | `/api/v1/persons`              | List Unblu persons                |
| GET    | `/api/v1/teams`                | List teams                        |
| GET    | `/api/v1/named-areas`          | List named areas                  |
| POST   | `/api/v1/webhooks/setup`       | Setup webhook                     |
| GET    | `/api/v1/webhooks/status`      | Webhook status                    |
| POST   | `/api/v1/webhooks/teardown`    | Remove webhook                    |

## Apache Camel Routes

Key internal routes (called via `direct:` component):

- `direct:start-conversation` — main conversation workflow
- `direct:start-direct-conversation` — direct chat workflow
- `direct:unblu-adapter-resilient` — Unblu API call with circuit breaker (timeout: 3000ms, fallback: `OFFLINE-PENDING`)
- `direct:erp-adapter` — ERP data enrichment (mock)
- `direct:rule-engine-adapter` — routing decisions (mock)
- `direct:conversation-summary-adapter` — summary generation (mock)

## Domain Model

Central pivot object enriched through orchestration:

- `ConversationContext` — main context object passed through Camel routes
- `CustomerProfile` — client data from ERP
- `ChatRoutingDecision` — routing rules result
- `PersonInfo` — Unblu participant (VIRTUAL or USER_DB)
- `TeamInfo` — agent queue/team

## Key Packages

```
org.dbs.poc.unblu.domain.model        → Domain entities
org.dbs.poc.unblu.application.port.in      → Use case interfaces (primary ports)
org.dbs.poc.unblu.domain.port.out     → Secondary port interfaces
org.dbs.poc.unblu.application.service → Camel routes implementing use cases
org.dbs.poc.unblu.infrastructure      → Adapter implementations
org.dbs.poc.unblu.exposition.rest     → REST routes and DTOs
```

## Mock Adapters (PoC Strategy)

Infrastructure layer uses mocks to enable development without external systems:

- `ExternalSystemsMockAdapters` — simulates ERP (customer segments: VIP, STANDARD, BANNED) and rule engine
- `ConversationSummaryMockAdapter` — random summaries (placeholder for LLM integration)
- `NgrokManager` — manages ngrok tunnel for webhook callbacks during local testing

Mocks implement the same port interfaces as real adapters — swap without code changes.

## Testing

```bash
mvn test                         # all backend tests
cd unblu-frontend && npm test    # Angular unit tests (Karma/Jasmine)
```

Tests are minimal (PoC phase). Existing test: `UnbluProxyConfigTest` validates proxy configuration.

## Monitoring

Spring Boot Actuator endpoints:

- `/actuator/health` — application health
- `/actuator/camelroutes` — Camel route status
- `/actuator/metrics` — application metrics

## Webhook Support

Webhooks require a publicly accessible URL. Use ngrok for local development:

- Endpoint: `/api/webhooks/unblu`
- Configured via `unblu.webhook.*` properties
- `WebhookSetupService` handles registration/teardown with Unblu

## Docs

- [
  `docs/ARCHITECTURE.md`](fleet-file://9t8iu3nimkkbb1vj16eb/Users/dmissud/MA/PocUnblu/PocUnblu/docs/ARCHITECTURE.md?type=file&root=%252F) —
  detailed architecture decisions
- [
  `docs/UNBLU_API_REFERENCE.md`](fleet-file://9t8iu3nimkkbb1vj16eb/Users/dmissud/MA/PocUnblu/PocUnblu/docs/UNBLU_API_REFERENCE.md?type=file&root=%252F) —
  Unblu API reference notes
- [
  `docs/WEBHOOK_IMPLEMENTATION_PLAN.md`](fleet-file://9t8iu3nimkkbb1vj16eb/Users/dmissud/MA/PocUnblu/PocUnblu/docs/WEBHOOK_IMPLEMENTATION_PLAN.md?type=file&root=%252F) —
  webhook implementation plan
