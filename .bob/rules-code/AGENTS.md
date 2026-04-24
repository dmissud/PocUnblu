# Project Coding Rules (Non-Obvious Only)

## Critical Patterns Discovered by Reading Code

### Server Configuration
- **NEVER enable server compression** (`server.compression.enabled: false` in application.yml) - Unblu-Hookshot sends `Accept-Encoding: gzip` but doesn't decompress responses, causing failures

### Camel Route Patterns
- **MUST use `.enrich()` with aggregation strategy** instead of `.toD()` when enriching ConversationContext - `.toD()` causes `NoTypeConversionAvailableException`
- Circuit breaker fallback returns `"OFFLINE-PENDING"` as conversation ID (not null or exception) - see [`UnbluApiAdapter.java:62`](../../unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/unblu/UnbluApiAdapter.java:62)

### Domain Model Enrichment Pattern
- [`ConversationContext`](../../unblu-domain/src/main/java/org/dbs/poc/unblu/domain/model/ConversationContext.java) uses **immutable constructor fields** (`initialClientId`, `originApplication`) + **mutable setters** for enrichment (`setCustomerProfile()`, `setRoutingDecision()`)
- This is intentional - not a mistake or inconsistency

### Unblu API Integration
- **MUST fetch internal Unblu person IDs** via `personsGetBySource()` before using in conversations - external source IDs don't work directly in conversation APIs
- **Bot creation order is critical**: Create person first, THEN create bot with `botPersonId` reference (reverse order fails)
- Map domain enums to Unblu SDK enums: `PersonSource.VIRTUAL` → `EPersonSource.VIRTUAL`

### Profile-Based Adapters
- Use `@Profile("ngrok")` for [`NgrokManager`](../../unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/NgrokManager.java:36)
- Use `@Profile("!ngrok")` for [`StaticTunnelAdapter`](../../unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/StaticTunnelAdapter.java:23)
- Only ONE tunnel adapter active at runtime based on profile

### Port/Adapter Pattern
- Port interfaces in `domain.port.in/out` packages
- Adapters use `@Component` or `@Service` + `@RequiredArgsConstructor` (Lombok)
- `@Primary` on main adapter when multiple implementations exist

### Code Style (Non-Standard)
- **Javadoc MUST be in French**, code/variables in English
- Domain records for immutable data (`CustomerProfile`, `ChatRoutingDecision`)
- Domain classes with business methods for entities (`ConversationContext.isChatAuthorized()`)

## Build Commands
- Debug profile: `mvn spring-boot:run -pl unblu-configuration -Dspring-boot.run.profiles=debug` (enables SQL + Camel verbose logging)
- Frontend: `./build-frontend.sh` (builds Angular → copies to `unblu-configuration/src/main/resources/static/browser/`)
