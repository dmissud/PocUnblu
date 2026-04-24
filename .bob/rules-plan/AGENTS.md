# Project Architecture Rules (Non-Obvious Only)

## Hidden Architectural Constraints

### Two Independent Hexagonal Systems
- **"Bloc 1" (integration-*)**: Webhook event processing - operates INDEPENDENTLY of Bloc 2
- **"Bloc 2" (unblu-*)**: Supervisor/orchestration - manages conversation lifecycle
- **Critical**: Bloc 1 can function without Bloc 2 (see [`conversation_history_processing.feature:27`](../../integration-application/src/test/resources/features/bloc1/conversation_history_processing.feature:27))
- NOT a single system split into modules - two separate bounded contexts

### Multi-Application Architecture (Non-Obvious)
- **Four Spring Boot applications** run simultaneously:
  - `unblu-configuration` (8081) - supervisor + Angular frontend serving
  - `webhook-entrypoint` (8083) - webhook receiver
  - `engine` (8084) - event processor
  - `livekit` (8082) - LiveKit proxy
- Each has its own `main()` method and can be deployed independently
- Communication via HTTP/Kafka between apps

### Domain Model Enrichment Pattern
- [`ConversationContext`](../../unblu-domain/src/main/java/org/dbs/poc/unblu/domain/model/ConversationContext.java) uses **intentional mixed mutability**:
  - Constructor fields (`initialClientId`, `originApplication`) are immutable
  - Enrichment fields (`customerProfile`, `routingDecision`) use setters
- This is NOT inconsistent design - it's the orchestration enrichment pattern
- Context flows through Camel routes, getting enriched at each step

### Camel Route Constraints
- **MUST use `.enrich()` with aggregation** for ConversationContext enrichment
- **CANNOT use `.toD()`** - causes `NoTypeConversionAvailableException` with domain objects
- Circuit breaker fallback returns `"OFFLINE-PENDING"` string (not null/exception) - downstream code expects this

### Unblu API Hidden Dependencies
- **Person IDs must be fetched first** via `personsGetBySource()` before using in conversations
- External source IDs don't work directly in conversation APIs (undocumented Unblu limitation)
- **Bot creation order matters**: Person → Bot (reverse fails silently or with cryptic errors)

### Profile-Based Adapter Switching
- `@Profile("ngrok")` vs `@Profile("!ngrok")` switches tunnel implementations
- Only ONE adapter active at runtime - NOT both
- [`NgrokManager`](../../unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/NgrokManager.java:36) vs [`StaticTunnelAdapter`](../../unblu-infrastructure/src/main/java/org/dbs/poc/unblu/infrastructure/adapter/ngrok/StaticTunnelAdapter.java:23)

### Testing Architecture (Non-Standard)
- **JUnit Platform Suite** pattern (not `@CucumberOptions`)
- Uses `@Suite` + `@IncludeEngines("cucumber")` + `@SelectClasspathResource`
- Features organized by "bloc" (bloc1/, bloc2/) not by feature type
- See [`Bloc1CucumberTest.java`](../../integration-application/src/test/java/org/dbs/poc/unblu/integration/application/bdd/Bloc1CucumberTest.java:1)

### Non-Obvious Performance Constraints
- **Server compression disabled globally** - Unblu-Hookshot incompatibility (sends gzip header but doesn't decompress)
- Enabling compression breaks bot outbound communication silently

### Frontend Integration (Hidden Coupling)
- Angular build output MUST go to `unblu-configuration/src/main/resources/static/browser/`
- [`build-frontend.sh`](../../build-frontend.sh:1) handles this automatically
- Frontend served by Spring Boot, not separate web server
- Proxy config in `proxy.conf.json` only for dev mode

## Planning Considerations
- When adding features, determine if they belong to Bloc 1 (event processing) or Bloc 2 (orchestration)
- New Camel routes should use `.enrich()` pattern for context enrichment
- New Unblu integrations must fetch person IDs before using in conversations
- Profile-based adapters require both implementations (active + inactive profiles)
