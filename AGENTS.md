# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Non-Obvious Build Commands
- Run with debug profile: `mvn spring-boot:run -pl unblu-configuration -Dspring-boot.run.profiles=debug` (enables SQL logging + verbose Camel)
- Frontend build script: `./build-frontend.sh` (builds Angular + copies to `unblu-configuration/src/main/resources/static/browser/`)
- Run single Cucumber test: Use `@Suite` + `@SelectClasspathResource("features/bloc1")` pattern (see [`Bloc1CucumberTest.java`](integration-application/src/test/java/org/dbs/poc/unblu/integration/application/bdd/Bloc1CucumberTest.java:1))

## Critical Non-Standard Patterns
- **Server compression disabled globally** (`server.compression.enabled: false`) because Unblu-Hookshot sends `Accept-Encoding: gzip` but doesn't decompress responses
- **ConversationContext enrichment**: Immutable constructor fields (`initialClientId`, `originApplication`), mutable setters for enrichment (`setCustomerProfile()`, `setRoutingDecision()`)
- **Camel enrichment**: MUST use `.enrich()` with aggregation strategy, NOT `.toD()` (avoids `NoTypeConversionAvailableException`)
- **Circuit breaker fallback**: Returns `"OFFLINE-PENDING"` conversation ID when Unblu unavailable (see [`UnbluApiAdapter.java`](unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluApiAdapter.java:62))
- **Unblu person lookup**: Always fetch internal IDs via `personsGetBySource()` before using in conversations (external IDs don't work directly)
- **Bot creation order**: Create person first, then bot with `botPersonId` reference

## Project-Specific Conventions
- **Javadoc in French**, code/variables in English
- **Domain records** for immutable data (`CustomerProfile`, `ChatRoutingDecision`), classes with business methods for entities
- **Port interfaces** in `domain.port.in/out`, adapters use `@Component` or `@Service` + `@RequiredArgsConstructor`
- **Ngrok profile**: Use `@Profile("ngrok")` for [`NgrokManager`](unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/NgrokManager.java:36), `@Profile("!ngrok")` for [`StaticTunnelAdapter`](unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/StaticTunnelAdapter.java:23)

## Testing Specifics
- **Cucumber BDD**: Features in `src/test/resources/features/`, step definitions in `bdd.steps` package
- **JUnit Platform Suite**: Use `@Suite` + `@IncludeEngines("cucumber")` + `@SelectClasspathResource` (not `@CucumberOptions`)
- **Test config**: `@ConfigurationParameter` for glue, plugins, filter tags

## Module Structure (Non-Obvious)
- **Two separate hexagonal systems**: "Bloc 1" (integration-*) for webhook processing, "Bloc 2" (unblu-*) for supervisor/orchestration
- **unblu-configuration**: Entry point + frontend serving (Angular output in `src/main/resources/static/browser/`)
- **Three Spring Boot apps**: `unblu-configuration` (8081), `webhook-entrypoint` (8083), `engine` (8084), `livekit` (8082)
