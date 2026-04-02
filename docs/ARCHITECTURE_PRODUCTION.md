# Architecture Hexagonale — Recommandations Production

> **Contexte :** Ce document analyse la mise en oeuvre de l'architecture hexagonale dans le PoC PocUnblu
> et définit les recommandations pour un passage en production. Il sert de référence pour la conception
> du système cible.

---

## 1. Analyse du PoC — État des lieux

### 1.1 Structure des modules

```
unblu-domain          → Modèles métier, interfaces de ports (zéro dépendance externe)
unblu-application     → Orchestration Apache Camel, implémentation des use cases
unblu-infrastructure  → Adapters techniques (Unblu API, JPA, mocks ERP/RuleEngine)
unblu-exposition      → Couche REST (Camel REST DSL, DTOs, mappers)
unblu-configuration   → Point d'entrée Spring Boot, assemblage des dépendances
unblu-frontend        → Angular 20 (UI de test)
```

### 1.2 Ports définis dans le domaine

**Ports secondaires (out) — correctement placés dans `unblu-domain` :**

| Interface | Rôle |
|-----------|------|
| `UnbluPort` | Accès à la plateforme Unblu (conversations, personnes, équipes) |
| `ErpPort` | Récupération du profil client |
| `RuleEnginePort` | Décision de routage |
| `ConversationSummaryPort` | Génération de résumé (LLM placeholder) |
| `ConversationHistoryRepository` | Persistance de l'historique des conversations |

**Ports primaires (in) — problème identifié :**

Les interfaces `StartConversationUseCase`, `EnrichConversationUseCase`, etc. existent dans
`unblu-application` mais **ne sont pas appelées directement par l'exposition**. L'exposition
route vers des noms de routes Camel (chaînes de caractères), ce qui crée un couplage implicite.

### 1.3 Flux actuel

```
REST Request
  ↓
RestExpositionRoute (Camel REST DSL)
  ↓  .to("direct:start-conversation")   ← couplage sur un nom de route
StartConversationRoute (Camel — application)
  ↓  processors internes
ConversationWorkflowService
  ↓  @Autowired
UnbluPort (port secondaire domaine)
  ↓  implémenté par
UnbluCamelAdapter (infrastructure)
```

### 1.4 Ce qui est bien fait

| Element | Evaluation |
|---------|------------|
| Ports secondaires dans le domaine | Correct — interfaces pures, zéro dépendance |
| Adapters infrastructure | Correct — implémentent les interfaces du domaine |
| Isolation des modules Maven | Correct — hiérarchie de dépendances respectée |
| Traitement des webhooks via Camel | Correct — retry, idempotence, exception handlers |
| DTO / Mapper par couche | Correct — pas de fuite de modèle entre couches |

### 1.5 Ce qui doit évoluer

| Problème | Impact |
|----------|--------|
| Ports primaires absents du domaine | L'exposition ne peut pas dépendre d'un contrat stable |
| Camel REST DSL en façade exposition | Couplage fort, stack traces opaques, tests difficiles |
| Camel comme orchestrateur synchrone | Complexité inutile — Resilience4j natif suffit |
| Noms de routes Camel comme contrats inter-couches | Couplage implicite non typé, refactoring risqué |

---

## 2. Le choix fondamental : deux natures de flux

Avant toute recommandation structurelle, il faut distinguer les deux natures de flux présents
dans ce type de système :

| Flux | Nature | Exemples |
|------|--------|---------|
| **Commandé** | Synchrone, request/response, initié par un acteur | `POST /conversations/start`, `GET /persons` |
| **Événementiel** | Asynchrone, fire-and-forget, initié par Unblu | Webhooks entrants, synchronisation batch |

**Camel est un outil d'intégration conçu pour le flux événementiel.**
Il est surdimensionné — et contre-productif — pour orchestrer des use cases synchrones.

> **Principe directeur :** Camel est cantonné à l'infrastructure événementielle.
> Les use cases synchrones sont des services Java injectables, testables unitairement.

### 2.1 Pourquoi pas Camel pour les use cases synchrones ?

#### Le modèle de programmation inadapté

Camel travaille sur un **Exchange** (message) qui transite dans une route. Pour séquencer
`erpPort → ruleEngine → unbluPort`, il faut stocker les résultats intermédiaires dans des headers
ou le body de l'Exchange, manipuler des processeurs sans signature typée, et accepter que le flux
soit piloté par le routage plutôt que par la logique métier.

Un service Java fait la même chose en 3 appels de méthodes, avec des types forts, lisibles et debuggables.

#### La testabilité est dégradée

Un use case en `@Service` se teste en deux lignes, sans Spring ni Camel :

```java

@Mock
UnbluPort unbluPort;
@InjectMocks
ConversationOrchestratorService service;
```

Une route Camel nécessite `CamelTestSupport`, le démarrage du contexte Camel et la simulation des
endpoints — beaucoup plus lourd pour tester de la logique synchrone.

#### Camel n'apporte rien que Java ne fasse mieux ici

| Besoin               | Camel                                    | Java pur                             |
|----------------------|------------------------------------------|--------------------------------------|
| Séquencer des appels | `.to("direct:step1").to("direct:step2")` | `step1(); step2();`                  |
| Résilience           | Camel Resilience4j component             | `@CircuitBreaker` Resilience4j natif |
| Gestion d'erreur     | `onException()`                          | `try/catch` ou `@ControllerAdvice`   |

#### Camel est pertinent quand il y a du routing réel

Camel brille pour les besoins **événementiels** :

- **Fan-out** : envoyer un message vers plusieurs systèmes en parallèle (`multicast`)
- **Retry / DLQ** : réessayer un traitement asynchrone avec back-off exponentiel
- **Idempotence** : déduplication de messages entrants
- **Transformation** : EIP patterns (split, aggregate, enrich)

Un use case synchrone comme `startConversation` n'a aucun de ces besoins. Utiliser Camel pour du
synchrone introduit une complexité accidentelle qui n'est justifiée par aucun bénéfice.

---

## 3. Architecture cible recommandée

### 3.1 Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────────┐
│  EXPOSITION                                                          │
│  @RestController Spring MVC                                          │
│  DTOs de requête/réponse + validation (@Valid)                       │
│  Mappers exposition → commandes domaine                              │
│  Dépend uniquement de : unblu-domain (port/in)                       │
│         │                                                            │
│         │  injection de dépendance (interface domaine)               │
│         ▼                                                            │
├─────────────────────────────────────────────────────────────────────┤
│  DOMAIN — port/in (ports primaires)                                  │
│  StartConversationUseCase                                            │
│  EnrichConversationUseCase                                           │
│  ListConversationHistoryUseCase                                      │
│  SearchConversationsByStateUseCase                                   │
│  ... (toutes les intentions métier exposées)                         │
│                                                                      │
│  DOMAIN — port/out (ports secondaires)                               │
│  UnbluPort  ErpPort  RuleEnginePort  ConversationSummaryPort         │
│  ConversationHistoryRepository                                       │
│                                                                      │
│  DOMAIN — model                                                      │
│  ConversationContext  CustomerProfile  ChatRoutingDecision ...        │
│         │                                                            │
│         │  implémente port/in, utilise port/out                      │
│         ▼                                                            │
├─────────────────────────────────────────────────────────────────────┤
│  APPLICATION                                                         │
│  ConversationOrchestratorService  implements StartConversationUseCase│
│  EnrichConversationService        implements EnrichConversationUseCase│
│  ConversationHistoryQueryService  implements ListConversation...     │
│  ... (un service par use case ou groupe cohérent)                    │
│                                                                      │
│  Resilience4j (@CircuitBreaker, @Retry) sur appels critiques         │
│  Pas de Camel ici pour les flux synchrones                           │
│         │                                                            │
│         │  implémentés par                                           │
│         ▼                                                            │
├─────────────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE                                                      │
│  UnbluApiAdapter   implements UnbluPort                              │
│  ErpAdapter        implements ErpPort                                │
│  RuleEngineAdapter implements RuleEnginePort                         │
│  JpaHistoryAdapter implements ConversationHistoryRepository          │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │  CAMEL — flux événementiels uniquement                     │     │
│  │  WebhookEventRoute  (retry, dead-letter, idempotence)      │     │
│  │  SyncConversationsRoute  (batch scheduling)                │     │
│  │  Ces routes appellent des use case interfaces (port/in)    │     │
│  └────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 Règles de dépendances Maven

```
exposition      →  domain  (port/in uniquement)
application     →  domain
infrastructure  →  domain
configuration   →  exposition, application, infrastructure
```

**Règle absolue :** `exposition` ne dépend **jamais** de `application` directement.
Elle injecte les interfaces de `domain/port/in`. Spring Boot fait le wiring dans `configuration`.

---

## 4. Implémentation couche par couche

### 4.1 Domain — ports primaires (in)

Les interfaces use case migrent dans `unblu-domain/src/main/java/.../domain/port/in/`.
Ce sont les **contrats stables** que toute couche externe peut consommer.

```java
// unblu-domain/port/in/StartConversationUseCase.java
public interface StartConversationUseCase {
    ConversationOrchestrationState execute(StartConversationCommand command);
}

// unblu-domain/port/in/EnrichConversationUseCase.java
public interface EnrichConversationUseCase {
    ConversationHistory execute(String conversationId);
}

// unblu-domain/port/in/ListConversationHistoryUseCase.java
public interface ListConversationHistoryUseCase {
    ConversationHistoryPage execute(ListConversationHistoryQuery query);
}
```

**Commandes et queries (CQRS léger) dans le domaine :**

```java
// unblu-domain/port/in/command/
public record StartConversationCommand(
    String clientId,
    String subject,
    String origin,
    String teamId
) {}

public record ListConversationHistoryQuery(
    int page,
    int size,
    String sortField,
    String sortDir
) {}
```

### 4.2 Exposition — @RestController standard

Remplacement du Camel REST DSL par des controllers Spring MVC classiques.

```java
// unblu-exposition/rest/ConversationController.java
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "Gestion des conversations Unblu")
public class ConversationController {

    private final StartConversationUseCase startConversation;
    private final EnrichConversationUseCase enrichConversation;
    private final ListConversationHistoryUseCase listHistory;
    private final ConversationMapper mapper;

    @PostMapping("/start")
    @Operation(summary = "Démarre une conversation avec une équipe")
    public ResponseEntity<StartConversationResponse> start(
            @RequestBody @Valid StartConversationRequest request) {

        var command = mapper.toCommand(request);
        var result  = startConversation.execute(command);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @PostMapping("/history/{id}/enrich")
    public ResponseEntity<ConversationHistoryDetailResponse> enrich(
            @PathVariable String id) {

        var history = enrichConversation.execute(id);
        return ResponseEntity.ok(mapper.toDetailResponse(history));
    }

    @GetMapping("/history")
    public ResponseEntity<ConversationHistoryPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt") String sortField,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        var query  = new ListConversationHistoryQuery(page, size, sortField, sortDir);
        var result = listHistory.execute(query);
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }
}
```

**Avantages du @RestController en production :**
- Stack traces lisibles, debug natif
- Tests d'intégration avec `MockMvc` (écosystème mature)
- OpenAPI/Swagger généré automatiquement par SpringDoc
- Validation `@Valid` + `@ControllerAdvice` pour la gestion d'erreurs
- Lisible pour tout développeur Java sans connaissance de Camel

### 4.3 Application — services d'orchestration

```java
// unblu-application/service/ConversationOrchestratorService.java
@Service
@Slf4j
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final UnbluPort unbluPort;
    private final ErpPort erpPort;
    private final RuleEnginePort ruleEnginePort;
    private final ConversationSummaryPort summaryPort;
    private final ConversationHistoryRepository historyRepository;

    @Override
    public ConversationOrchestrationState execute(StartConversationCommand command) {
        // 1. Enrichissement client via ERP
        var profile = erpPort.getCustomerProfile(command.clientId());

        // 2. Décision de routage
        var decision = ruleEnginePort.evaluateRouting(command, profile);
        if (!decision.isAuthorized()) {
            return ConversationOrchestrationState.unauthorized(decision.reason());
        }

        // 3. Création conversation Unblu (avec resilience)
        var conversation = createConversationResilient(command, decision);

        // 4. Génération et ajout du résumé
        var summary = summaryPort.generateSummary(conversation.id());
        unbluPort.addSummaryToConversation(conversation.id(), summary);

        return ConversationOrchestrationState.success(conversation, profile, decision);
    }

    @CircuitBreaker(name = "unblu", fallbackMethod = "createConversationFallback")
    @Retry(name = "unblu")
    private UnbluConversationInfo createConversationResilient(
            StartConversationCommand command,
            ChatRoutingDecision decision) {
        return unbluPort.createConversation(command, decision);
    }

    private UnbluConversationInfo createConversationFallback(
            StartConversationCommand command,
            ChatRoutingDecision decision,
            Exception ex) {
        log.warn("Unblu indisponible, création en mode dégradé", ex);
        return UnbluConversationInfo.offline(command.clientId());
    }
}
```

**Pourquoi Resilience4j natif plutôt que Camel :**
- Annotations déclaratives (`@CircuitBreaker`, `@Retry`, `@TimeLimiter`)
- Configuration dans `application.yaml` par service nommé
- Métriques exposées nativement dans Spring Boot Actuator
- Pas de dépendance supplémentaire (inclus dans Spring Boot 3.x)

### 4.4 Infrastructure — adapters Unblu sans Camel

Pour les flux synchrones, l'adapter Unblu n'a pas besoin de passer par Camel :

```java
// unblu-infrastructure/adapter/out/UnbluApiAdapter.java
@Component
public class UnbluApiAdapter implements UnbluPort {

    private final UnbluConversationService conversationService;
    private final UnbluPersonService personService;
    private final UnbluService unbluService;

    @Override
    public UnbluConversationInfo createConversation(
            StartConversationCommand command,
            ChatRoutingDecision decision) {
        return conversationService.create(command, decision);
    }

    @Override
    public List<PersonInfo> searchPersons(String sourceId, PersonSource source) {
        return personService.search(sourceId, source);
    }

    // ... autres méthodes
}
```

### 4.5 Infrastructure — Camel cantonné à l'événementiel

```java
// unblu-infrastructure/adapter/in/WebhookEventRoute.java
@Component
public class WebhookEventRoute extends RouteBuilder {

    // Injection de use case interfaces — pas de services applicatifs directs
    private final ConversationEventHandler conversationEventHandler;

    @Override
    public void configure() {
        // Dead Letter Queue pour les événements non traitable
        errorHandler(deadLetterChannel("{{camel.webhook.dlq}}")
            .maximumRedeliveries(5)
            .redeliveryDelay(1000)
            .backOffMultiplier(2.0)
            .useExponentialBackOff()
            .log(log));

        from("direct:webhook-event")
            // Idempotence par ID webhook
            .idempotentConsumer(
                header("X-Webhook-Id"),
                MemoryIdempotentRepository.memoryIdempotentRepository(5000))
            // Routing par type d'événement
            .choice()
                .when(header("eventType").startsWith("conversation"))
                    .bean(conversationEventHandler)
                .when(header("eventType").startsWith("person"))
                    .bean(personEventHandler)
                .otherwise()
                    .log(LoggingLevel.WARN, "Événement webhook inconnu: ${header.eventType}")
            .end();
    }
}
```

---

## 5. Gestion des erreurs en production

### 5.1 Stratégie par couche

| Couche | Mécanisme | Exemple |
|--------|-----------|---------|
| Exposition | `@ControllerAdvice` global | Traduit les exceptions domaine en HTTP |
| Application | Exceptions domaine typées | `ConversationNotFoundException`, `UnauthorizedException` |
| Infrastructure sync | `@CircuitBreaker` / `@Retry` Resilience4j | Appels Unblu, ERP, RuleEngine |
| Infrastructure async | Camel error handler + DLQ | Traitement webhooks |

### 5.2 Hiérarchie d'exceptions domaine

```java
// unblu-domain/exception/
public class DomainException extends RuntimeException { ... }
public class ConversationNotFoundException extends DomainException { ... }
public class UnauthorizedConversationException extends DomainException { ... }
public class UnbluUnavailableException extends DomainException { ... }
```

```java
// unblu-exposition/rest/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ConversationNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedConversationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedConversationException ex) {
        return ResponseEntity.status(403).body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(UnbluUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUnavailable(UnbluUnavailableException ex) {
        return ResponseEntity.status(503).body(ErrorResponse.of("Service temporairement indisponible"));
    }
}
```

---

## 6. Observabilité en production

### 6.1 Tracing distribué

Avec Spring Boot 3.x, Micrometer Tracing est intégré nativement.
Chaque use case produit un span tracé automatiquement.

```yaml
# application.yaml
management:
  tracing:
    sampling:
      probability: 1.0
  endpoints:
    web:
      exposure:
        include: health,info,metrics,camelroutes,prometheus
```

### 6.2 Métriques par use case

```java
@Service
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final MeterRegistry meterRegistry;

    @Override
    public ConversationOrchestrationState execute(StartConversationCommand command) {
        var timer = Timer.start(meterRegistry);
        try {
            var result = doExecute(command);
            meterRegistry.counter("conversation.start", "status", "success").increment();
            return result;
        } catch (Exception e) {
            meterRegistry.counter("conversation.start", "status", "error",
                "type", e.getClass().getSimpleName()).increment();
            throw e;
        } finally {
            timer.stop(meterRegistry.timer("conversation.start.duration"));
        }
    }
}
```

---

## 7. Tests en production

### 7.1 Stratégie de test par couche

```
Exposition  → MockMvc (test de contrat HTTP, validation, codes retour)
Application → Test unitaire pur (mock des ports avec Mockito)
Domain      → Test unitaire pur (logique métier isolée)
Infrastructure → Test d'intégration (Testcontainers pour PostgreSQL, WireMock pour Unblu)
Camel routes  → CamelTestSupport (test des routes événementielles)
```

### 7.2 Test d'un use case (couche application)

```java
@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorServiceTest {

    @Mock UnbluPort unbluPort;
    @Mock ErpPort erpPort;
    @Mock RuleEnginePort ruleEnginePort;
    @Mock ConversationSummaryPort summaryPort;

    @InjectMocks
    ConversationOrchestratorService service;

    @Test
    void should_create_conversation_when_authorized() {
        // given
        var command = new StartConversationCommand("client-1", "Aide", "web", "team-A");
        given(erpPort.getCustomerProfile("client-1")).willReturn(CustomerProfile.vip("client-1"));
        given(ruleEnginePort.evaluateRouting(any(), any())).willReturn(ChatRoutingDecision.authorized("team-A"));
        given(unbluPort.createConversation(any(), any())).willReturn(UnbluConversationInfo.of("conv-123"));

        // when
        var result = service.execute(command);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.conversationId()).isEqualTo("conv-123");
        verify(unbluPort).addSummaryToConversation(eq("conv-123"), any());
    }
}
```

### 7.3 Test d'exposition (MockMvc)

```java
@WebMvcTest(ConversationController.class)
class ConversationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean StartConversationUseCase startConversation;

    @Test
    void should_return_201_on_successful_start() throws Exception {
        given(startConversation.execute(any()))
            .willReturn(ConversationOrchestrationState.success(...));

        mockMvc.perform(post("/api/v1/conversations/start")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"clientId": "c-1", "subject": "Aide", "origin": "web"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conversationId").exists());
    }
}
```

---

## 8. Migration depuis le PoC

### 8.1 Plan de migration par étape

| Etape | Action | Risque |
|-------|--------|--------|
| 1 | Déplacer les interfaces use case dans `domain/port/in/` | Faible — refactoring de package |
| 2 | Créer les `@RestController` en parallèle des routes Camel existantes | Faible — les deux coexistent |
| 3 | Connecter les controllers aux use case interfaces | Faible — wiring Spring |
| 4 | Supprimer le Camel REST DSL | Moyen — retirer le routage existant |
| 5 | Remplacer les `ProducerTemplate` synchrones par appels directs | Moyen — simplification |
| 6 | Ajouter Resilience4j sur les appels critiques | Faible — annotations |
| 7 | Garder et renforcer les routes Camel événementielles | Faible — amélioration |

### 8.2 Ce qui ne change pas

- Les ports secondaires (`UnbluPort`, `ErpPort`, etc.) — déjà bien placés
- Les adapters infrastructure — déjà conformes
- La route webhook Camel — déjà dans le bon paradigme
- Les DTOs et mappers — à adapter légèrement pour MockMvc
- La gestion de la persistance JPA — déjà correcte

---

## 9. Tableau de décision : Camel vs Java pur

| Scénario | Recommandation | Raison |
|----------|---------------|--------|
| Use case synchrone simple | Java pur + `@Service` | Lisibilité, testabilité |
| Use case avec retry/circuit breaker | Java + Resilience4j annotations | Natif Spring Boot, métriques incluses |
| Traitement webhook asynchrone | Camel route | Retry, DLQ, idempotence natifs |
| Synchronisation batch | Camel route + Quartz | Scheduling intégré |
| Transformation de message complexe | Camel route | EIP patterns (split, aggregate, filter) |
| Appel REST externe simple | `RestClient` Spring (Java pur) | Suffisant, plus lisible |
| Fan-out vers plusieurs systèmes | Camel route (`multicast`) | Parallélisme natif |
| Orchestration long-running (saga) | Camel route avec persistance | State management intégré |

---

## 10. Résumé des principes directeurs

1. **Les ports primaires (in) sont dans le domaine.** Toute couche peut en dépendre sans coupler vers l'implémentation.

2. **L'exposition est du Spring MVC standard.** Elle ne connaît pas Camel, ne connaît pas les services — seulement les interfaces de domaine.

3. **Camel est un outil d'infrastructure, pas un framework applicatif.** Il gère les flux événementiels, pas les use cases synchrones.

4. **La résilience est déclarative avec Resilience4j.** `@CircuitBreaker` et `@Retry` sur les méthodes d'application qui appellent des systèmes externes.

5. **Un use case = une interface = un service.** La granularité est métier, pas technique.

6. **Les routes Camel événementielles appellent des use case interfaces.** Elles ne contiennent pas de logique métier — elles délèguent.

7. **Les tests unitaires de la couche application ne démarrent pas Spring.** Ils mockent les ports avec Mockito. Rapides, fiables, exhaustifs.

---

*Document produit le 2026-04-02 — basé sur l'analyse du PoC PocUnblu et les recommandations architecture production.*
