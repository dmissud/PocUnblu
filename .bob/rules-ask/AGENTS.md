# Project Documentation Rules (Non-Obvious Only)

## Non-Obvious Architecture Context

### Two Separate Hexagonal Systems
- **"Bloc 1"** (integration-*): Webhook processing layer - processes Unblu events independently
- **"Bloc 2"** (unblu-*): Supervisor/orchestration layer - manages conversation lifecycle
- These are SEPARATE systems with different domains, not a single monolith split into modules

### Module Naming Confusion
- `unblu-configuration` is NOT just config - it's the **Spring Boot entry point** + **frontend serving** (Angular output in `src/main/resources/static/browser/`)
- `integration-*` modules are NOT generic integration - they're specifically for webhook event processing
- `unblu-*` modules handle supervisor/orchestration, not just Unblu API calls

### Multiple Spring Boot Applications
- **Three separate apps** run on different ports:
  - `unblu-configuration` (8081) - main supervisor + frontend
  - `webhook-entrypoint` (8083) - webhook receiver
  - `engine` (8084) - event processor
  - `livekit` (8082) - LiveKit proxy
- NOT a single monolithic application

### Testing Structure (Non-Standard)
- **Cucumber BDD** features in `src/test/resources/features/` (standard location)
- **JUnit Platform Suite** pattern: Use `@Suite` + `@IncludeEngines("cucumber")` + `@SelectClasspathResource`
- **NOT using `@CucumberOptions`** (older pattern) - uses `@ConfigurationParameter` instead
- Example: [`Bloc1CucumberTest.java`](../../integration-application/src/test/java/org/dbs/poc/unblu/integration/application/bdd/Bloc1CucumberTest.java:1)

### Documentation Locations
- Architecture docs split by concern:
  - `docs/integration/` - Bloc 1 (webhook processing)
  - `docs/orchestration/` - Bloc 2 (supervisor)
- `CLAUDE.md` contains detailed setup instructions (not just AI rules)
- `FRONTEND_GUIDE.md` exists for Angular-specific guidance

### Non-Obvious Configuration
- **Server compression globally disabled** - not a mistake, required for Unblu-Hookshot compatibility
- **Debug profile** enables SQL logging + verbose Camel (not just log level changes)
- **Ngrok profile** switches tunnel adapter implementation (not just config values)

### Code Style Context
- **French Javadoc** is intentional (not a translation error) - project convention
- **Mixed immutable/mutable pattern** in ConversationContext is by design for enrichment workflow
- **Domain records** vs **domain classes** distinction is architectural (immutable data vs entities with behavior)

## Key Non-Obvious Files
- [`build-frontend.sh`](../../build-frontend.sh:1) - builds Angular AND copies to Spring Boot resources (not just npm build)
- [`application-debug.yml`](../../unblu-configuration/src/main/resources/application-debug.yml:1) - enables SQL + Camel verbose logging
- [`ConversationContext.java`](../../unblu-domain/src/main/java/org/dbs/poc/unblu/domain/model/ConversationContext.java:1) - demonstrates enrichment pattern
