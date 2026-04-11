# Architecture — Intégration Unblu

> **Prérequis :** lire [`../ARCHITECTURE_OVERVIEW.md`](../ARCHITECTURE_OVERVIEW.md) pour le contexte
> global et le découpage en deux blocs.

**Documents détaillés de ce bloc :**

| Document | Périmètre |
|----------|-----------|
| [`KAFKA.md`](./KAFKA.md) | Topics, producteur, consommateur, DLQ, variables d'environnement |
| [`unblu-adapter-doc/index.md`](./unblu-adapter-doc/index.md) | Usages Unblu SDK (IntegrationUnbluPort, bot dialog) |
| [`WEBHOOK_IMPLEMENTATION_PLAN.md`](./WEBHOOK_IMPLEMENTATION_PLAN.md) | Plan d'implémentation du pipeline webhook |

---

## Bloc 1 — Orchestration

### Modules

| Module | Responsabilité |
|--------|----------------|
| `unblu-domain` | Modèles métier purs (`ConversationContext`, `CustomerProfile`, `ChatRoutingDecision`, `PersonInfo`, `TeamInfo`), interfaces de ports IN et OUT, zéro dépendance externe |
| `unblu-application` | Routes Camel implémentant les use cases, processors webhook, service d'orchestration |
| `unblu-infrastructure` | Adapters techniques : SDK Unblu, enregistrement bot/webhook, tunnel ngrok |
| `unblu-exposition` | Exposition REST (Camel REST DSL + Spring controllers), proxies vers `livekit` |
| `unblu-configuration` | Point d'entrée `@SpringBootApplication`, assemblage, config Spring Boot (port 8081) |
| `livekit` | Microservice bot autonome (port 8082) |
| `webhook-entrypoint` | Microservice de réception webhook → Kafka (port 8083) |

### Démarrage d'une conversation (use case principal)

```
POST /api/v1/conversations/start
  ↓
RestExpositionRoute (Camel REST DSL) ou ConversationController (Spring)
  ↓  direct:start-conversation
StartConversationRoute
  ↓
ConversationWorkflowService
  ├── ErpPort → direct:erp-adapter (mock : CustomerProfile VIP/STANDARD/BANNED)
  ├── RuleEnginePort → direct:rule-engine-adapter (mock : ChatRoutingDecision)
  └── UnbluPort → UnbluCamelAdapterPort
                    ↓
                  direct:unblu-adapter-resilient (circuit breaker 3 000 ms)
                    ↓
                  direct:unblu-adapter → UnbluConversationService → SDK Unblu
```

Le fallback du circuit breaker retourne `conversationId = "OFFLINE-PENDING"` dans le
`ConversationOrchestrationState`.

**Quatre routes résilientes couvrent toutes les opérations Unblu :**

| Route résiliente | Fallback |
|-----------------|---------|
| `direct:unblu-adapter-resilient` | `OFFLINE-PENDING` dans `ConversationOrchestrationState` |
| `direct:unblu-search-persons-resilient` | Liste vide |
| `direct:unblu-create-direct-conversation-resilient` | `ConversationData` avec id `OFFLINE-PENDING` |
| `direct:unblu-add-summary-resilient` | Log silencieux |

### Webhook — réception et publication Kafka

```
Unblu (plateforme)
  → HTTPS ngrok → webhook-entrypoint:8083
  → WebhookReceiverController
      ├── Validation (corps non vide, header X-Unblu-Event présent)
      ├── KafkaTemplate.send(topic="unblu.webhook.events", key=eventType, value=jsonBody)
      └── 202 Accepted (traitement asynchrone)
```

Le service `webhook-entrypoint` est volontairement minimal : il ne fait aucun traitement métier,
il produit le message Kafka et acquitte immédiatement Unblu.

### Bot (livekit)

Le microservice `livekit` (port 8082) gère l'interaction dialogue avec le bot Unblu (PocBot).

```
Unblu Hookshot
  → HTTPS ngrok → livekit:8082 (ou proxy unblu-configuration:8081)
  → BotOutboundController.handle(serviceName, rawBody)
      ├── onboarding_offer / reboarding_offer → acceptBoarding()
      ├── offboarding_offer → acceptOffboarding()
      ├── dialog.opened → PocBotDialogService.onDialogOpened() [asynchrone]
      │     ├── setNamedAreaRecipient(conversationId)
      │     ├── ConversationSummaryPort.generateSummary()
      │     ├── BotsApi.sendTextMessage()
      │     └── BotsApi.handOffToAgent()
      ├── dialog.message → PocBotDialogService.onDialogMessage() [asynchrone]
      └── dialog.closed → ack immédiat
```

**Point critique :** `livekit` désactive la compression HTTP (`server.compression.enabled: false`)
car Unblu-Hookshot ne décompresse pas les réponses gzip. Voir
[`ARCHITECTURE_OUTILLAGE.md`](./ARCHITECTURE_OUTILLAGE.md) pour le détail.

### Proxies (unblu-configuration / unblu-exposition)

En profil `ngrok`, `unblu-configuration` (port 8081) fait office de proxy inverse :

| Proxy | Source | Destination |
|-------|--------|-------------|
| `LiveKitProxyController` | `/api/livekit/**` → 8081 | livekit:8082 |
| `BotOutboundProxyController` | `/api/bot/outbound` → 8081 | livekit:8082 |

Le proxy utilise `ProxyHeaders.extract()` qui filtre les headers problématiques, notamment
`Accept-Encoding` (évite la réponse gzip).

---

## Bloc 2 — Traitement & Historique

### Modules

| Module | Responsabilité |
|--------|----------------|
| `integration-domain` | Modèles historique (`ConversationHistory`, `ConversationEventHistory`, `ParticipantHistory`), ports OUT (`ConversationHistoryRepository`, `IntegrationUnbluPort`, `ConversationSummaryPort`) |
| `integration-application` | Routes Camel processors, services `ConversationHistoryService`, `SyncConversationsService`, `EnrichConversationService` |
| `integration-infrastructure` | Adapters JPA (Liquibase, PostgreSQL), adapter Unblu client, mock résumé |
| `engine` | Point d'entrée Spring Boot + consumer Kafka (port 8084) |

### Consommation Kafka et traitement webhook

```
topic: unblu.webhook.events
  ↓
KafkaWebhookConsumerRoute (groupId=engine, autoOffsetReset=earliest)
  ↓
Désérialisation JSON → UnbluWebhookPayload
  ↓
direct:webhook-event-processor
  ↓
WebhookEventRoute (dispatcher)
  ├── eventType starts with "conversation." → direct:webhook-handle-conversation
  │     → ConversationEventProcessor → ConversationHistoryService
  │           → ConversationHistoryRepository → PostgreSQL
  ├── eventType starts with "person."       → direct:webhook-handle-person
  │     → PersonEventProcessor
  └── otherwise                             → direct:webhook-handle-unknown
            → UnknownEventProcessor (log)
```

**Gestion des erreurs dans `KafkaWebhookConsumerRoute` :**

| Type d'erreur | Comportement |
|---------------|-------------|
| `IllegalArgumentException` (payload invalide) | DLQ immédiat, sans retry |
| `Exception` générale (erreur transitoire) | 3 retries (back-off exponentiel 2s × 2), puis DLQ |

Les messages en DLQ sont enrichis avec : `dlq.original.topic`, `dlq.error.message`,
`dlq.error.class`, `dlq.failed.at`, `dlq.retry.count`.

**Gestion des doublons dans `WebhookEventRoute` :**

`DataIntegrityViolationException` est capturée et ignorée (idempotence par contrainte unique en
base). Les erreurs métier (`IllegalArgumentException`) sont loggées sans retry.

### Modèle de persistance (ConversationHistory)

```
ConversationHistory (entité racine)
  ├── conversationId (clé métier)
  ├── topic, state (CREATED → ACTIVE → ENDED)
  ├── startedAt, endedAt
  ├── List<ParticipantHistory>   (participants enregistrés)
  └── List<ConversationEventHistory>  (événements horodatés)
```

`ConversationHistory` est une entité riche avec invariants métier :
- `create()` — usine statique
- `end(Instant)` — rejette si déjà terminée
- `recordMessage(...)` — rejette si conversation terminée
- `registerParticipant(...)` — idempotent

### API de consultation (engine)

`ConversationHistoryController` expose une API REST paginée et triée pour consulter l'historique.
Elle est consommée par le frontend Angular (module `unblu-frontend`).

---

## Décision architecturale : deux architectures hexagonales

Le projet applique deux instances de l'architecture hexagonale avec des approches complémentaires :

**Bloc 1 (Orchestration) — approche pragmatique**

La route Camel principale _est_ la règle métier (flux d'orchestration = séquence d'appels). Camel
orchestre directement les adapters via `direct:` endpoints. Les ports IN/OUT existent mais le
couplage passe par les noms de routes Camel.

**Bloc 2 (Historique) — approche stricte**

Les ports IN (`GetConversationHistoryUseCase`, `ListConversationHistoryUseCase`, etc.) sont des
interfaces Java appelées directement par les adapters primaires. Les ports OUT
(`ConversationHistoryRepository`, `IntegrationUnbluPort`) sont implémentés par des adapters JPA.
Le domaine est testable en isolation avec des mocks.

Ce choix est documenté dans [`orchestration_architecture.md`](./orchestration_architecture.md).