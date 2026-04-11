# Architecture — Vue d'ensemble

## Objectif du projet

PocUnblu démontre l'intégration de la plateforme [Unblu](https://www.unblu.com/) dans un système
d'entreprise. Il orchestre des conversations temps réel (chat, bot) tout en persistant les
événements pour l'historique et l'audit.

---

## Deux blocs indépendants

Le projet est structuré en deux blocs fonctionnels distincts, chacun avec ses propres modules Maven,
ses propres applications Spring Boot et sa propre responsabilité.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  BLOC ORCHESTRATION (démarrage de conversations, exposition REST)           │
│                                                                             │
│  unblu-domain        → modèles métier, ports (zéro dépendance externe)     │
│  unblu-application   → use cases, routes Camel                             │
│  unblu-infrastructure→ adapters Unblu SDK, ngrok, bot registration         │
│  unblu-exposition    → REST public + proxies (vers livekit, bot outbound)  │
│  unblu-configuration → point d'entrée Spring Boot, assemblage              │
└─────────────────────────────────────────────────────────────────────────────┘
          │ Kafka topic: unblu.webhook.events (produit par webhook-entrypoint)
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  BLOC INTEGRATION (réception webhook, bot, traitement événements,           │
│                    persistance historique)                                  │
│                                                                             │
│  integration-domain        → modèles, ports de sortie (historique)         │
│  integration-application   → routes Camel, processors, services historique │
│  integration-infrastructure→ JPA, Liquibase, adapter Unblu client          │
│  webhook-entrypoint  → reçoit les webhooks Unblu → publie sur Kafka        │
│  livekit             → microservice bot (dialog Unblu, outbound handler)   │
│  engine              → consumer Kafka, persistance, API historique         │
└─────────────────────────────────────────────────────────────────────────────┘
```

Les deux blocs partagent **Kafka** comme canal d'intégration asynchrone et **PostgreSQL** comme
base de données commune.

---

## Principe architectural : Hexagonal (Ports & Adapters)

Chaque bloc suit la même discipline :

```
Adapters primaires (IN)          Domaine / Application            Adapters secondaires (OUT)
────────────────────             ───────────────────────          ──────────────────────────
REST controllers         →       Ports IN (interfaces)    →       Ports OUT (interfaces)
Camel routes REST DSL    →       Use cases / Routes Camel →       Unblu SDK, JPA, Kafka
Kafka consumer           →       Processors               →       Mocks (ERP, Rule Engine)
```

**Règle fondamentale :** le domaine ne dépend de rien d'externe. Il définit des interfaces (ports)
que les adapters implémentent. On peut substituer n'importe quel adapter sans toucher au domaine.

---

## Flux de bout en bout

### Démarrage d'une conversation (Orchestration)

```
Client HTTP
  → POST /api/v1/conversations/start
  → unblu-exposition (REST adapter)
  → unblu-application (route Camel: direct:start-conversation)
  → ConversationWorkflowService (enrichissement ERP, décision routage)
  → UnbluPort → UnbluCamelAdapterPort → UnbluResilientRoute (circuit breaker)
  → SDK Unblu → API Unblu REST v4
  ← conversationId + joinUrl
```

### Réception d'un webhook Unblu (Integration)

```
Unblu (plateforme)
  → HTTPS vers tunnel ngrok (webhook-entrypoint:8083)
  → WebhookReceiverController
  → KafkaTemplate → topic unblu.webhook.events
  ← 202 Accepted (immédiat)

topic unblu.webhook.events
  → KafkaWebhookConsumerRoute (engine)
  → désérialisation UnbluWebhookPayload
  → direct:webhook-event-processor
  → WebhookEventRoute (dispatcher par type d'événement)
  → ConversationEventProcessor ou PersonEventProcessor
  → ConversationHistoryService → ConversationHistoryRepository → PostgreSQL
```

### Interaction bot (Integration — livekit)

```
Unblu (Hookshot)
  → POST /api/bot/outbound (via tunnel ngrok livekit:8082)
  → BotOutboundController (livekit)
  → PocBotDialogService (asynchrone)
     → ConversationSummaryPort (génère un résumé)
     → BotsApi Unblu (sendTextMessage, handOffToAgent)
```

---

## Infrastructure partagée

| Composant  | Rôle                                                       |
|------------|------------------------------------------------------------|
| PostgreSQL  | Persistance de l'historique des conversations              |
| Kafka       | Bus d'événements webhook (découplage Bloc 1 / Bloc 2)      |
| Ngrok       | Tunnels HTTPS pour exposer les services locaux à Unblu     |

---

## Documents détaillés

| Document | Périmètre |
|----------|-----------|
| [`orchestration/ARCHITECTURE.md`](./orchestration/ARCHITECTURE.md) | Modules unblu-* : hexagonale, routes Camel, adaptateur Unblu, circuit breaker |
| [`orchestration/AUDIT_TECHNICIEN.md`](./orchestration/AUDIT_TECHNICIEN.md) | Audit qualité, sprints complétés, dettes techniques restantes |
| [`orchestration/unblu-adapter-doc/index.md`](./orchestration/unblu-adapter-doc/index.md) | Adaptateur Unblu en détail (conversations, personnes, bots, sync) |
| [`integration/ARCHITECTURE_INTEGRATION.md`](./integration/ARCHITECTURE_INTEGRATION.md) | Modules integration-* : webhook, bot, engine, Kafka, persistance historique |
| [`ARCHITECTURE_OUTILLAGE.md`](./ARCHITECTURE_OUTILLAGE.md) | Ngrok, Docker Compose, profils, gzip, gestion des exceptions Camel |