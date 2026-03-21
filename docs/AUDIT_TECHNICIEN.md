# Audit Technique — PocUnblu
**Date initiale :** 2026-03-20 | **Mise à jour :** 2026-03-21
**Auteur :** Tech Lead Senior
**Périmètre :** Architecture hexagonale, DDD, Clean Code, Apache Camel

---

## Résumé Exécutif

Le projet présente une **base architecturale solide** avec une structure hexagonale bien organisée et un découpage modulaire Maven cohérent. Le proof-of-concept atteint son objectif d'intégration Unblu.

**Sprints 1, 2 et 3 complétés.** Les problèmes bloquants et les dettes majeures ont été traités. Le Sprint 4 (tests) est volontairement différé — décision stratégique avant une session de découverte API avec des développeurs où les tests auraient freiné sans apporter de valeur immédiate.

### Évolution du score

| Domaine               | Score initial | Score actuel |
|-----------------------|:-------------:|:------------:|
| Architecture DDD      | 7/10          | **8.5/10**   |
| Clean Code            | 6/10          | **8/10**     |
| Camel Best Practices  | 5.5/10        | **8/10**     |
| Persistance           | 5/10          | **8.5/10**   |
| Testabilité           | 3/10          | **5/10**     |
| **Score global**      | **6.5/10**    | **7.5/10**   |

> La testabilité progresse (architecture maintenant testable) mais reste limitée par l'absence de tests écrits — dette assumée.

---

## 1. Architecture & DDD

### ✅ Ce qui est bien fait

**Séparation des modules Maven**
Le découpage `domain → application → infrastructure → exposition → configuration` respecte fidèlement la dépendance hexagonale. Le domaine ne dépend de rien d'externe.

**Value Objects avec Records Java**
`CustomerProfile`, `ChatRoutingDecision`, `PersonInfo`, `TeamInfo` sont des records immuables avec validation dans le constructeur compact.

```java
public record ChatRoutingDecision(boolean isAuthorized, String unbluAssignedGroupId, String routingReason) {
    public ChatRoutingDecision {
        if (isAuthorized) {
            Objects.requireNonNull(unbluAssignedGroupId, "unbluAssignedGroupId must be provided for authorized chats");
        }
    }
}
```

**Ports & Adapters clairement définis**
`UnbluPort`, `ErpPort`, `RuleEnginePort`, `ConversationSummaryPort`, `ConversationHistoryRepository` dans `domain.port.out` — interfaces propres, aucune dépendance vers l'infrastructure.

**Commands comme objets immuables**
`StartConversationCommand` et `StartDirectConversationCommand` sont des records avec validation.

---

### ⚠️ Problèmes identifiés

#### DDD-01 — `ConversationHistory` est une entité anémique ✅ RÉSOLU (P2-03)

`ConversationHistory` est maintenant une entité riche avec invariants métier :
- Usine statique `create()`
- `end(Instant)` — rejette si déjà terminée
- `recordMessage(...)` — rejette si conversation terminée
- `registerParticipant(...)` — idempotent
- Accesseurs intent-revealing sans préfixe `get`

#### DDD-02 — `ConversationContext` portait l'état infrastructure ✅ RÉSOLU (P3-03)

`ConversationContext` (domaine) est maintenant un objet pur : clientId, origine, profil client, décision de routage.
`ConversationOrchestrationState` (application) porte les résultats Camel : `unbluConversationId`, `unbluJoinUrl`.

#### DDD-03 — Ports secondaires dans le mauvais package ✅ RÉSOLU (P2-04)

Tous les ports secondaires sont dans `domain.port.out`.

#### DDD-04 — Absence de Bounded Context explicite
**Sévérité : Faible (PoC)**

Le projet mélange le sous-domaine "orchestration de conversation" et "historique/audit". Acceptable au stade PoC. À adresser si le projet passe en production (voir chapitre 7).

---

## 2. Clean Code

### ✅ Ce qui est bien fait

- Nommage explicite et cohérent
- Constantes dans `OrchestratorEndpoints`
- Records Java pour les Value Objects
- `@RequiredArgsConstructor` + injection par constructeur
- `GlobalExceptionHandler` centralisé
- Accesseurs intent-revealing sur les entités domaine (sans préfixe `get`)

---

### ⚠️ Problèmes identifiés

#### CC-01 — `ConversationEventProcessor` : classe God ✅ RÉSOLU (P2-02)

Décomposée en :
- `ConversationEventProcessor` — dispatcher pur (~50 lignes)
- `ConversationHistoryService` — logique métier `@Transactional`
- `WebhookEventTypeExtractor` — extraction du type depuis `UnbluWebhookPayload`

#### CC-02 — Parsing de `Map<String, Object>` répété partout ✅ RÉSOLU (P2-01)

Le payload webhook est désérialisé en `UnbluWebhookPayload` (record typé) dès `WebhookReceiverRoute`. Tous les composants aval reçoivent des objets typés.

#### CC-03 — `@Transactional` sur méthode `private` ✅ RÉSOLU (P1-01)

Les méthodes transactionnelles sont dans `ConversationHistoryService` (service Spring dédié), annotées au niveau classe.

#### CC-04 — `UnbluService` : classe trop longue ✅ RÉSOLU (P3-01)

Décomposée en :
- `UnbluPersonService` — personnes (search, getBySource, agents)
- `UnbluConversationService` — conversations (create, createDirect, addSummary)
- `UnbluBotService` — gestion des bots
- `UnbluWebhookService` — webhooks (CRUD)
- `UnbluService` — résiduel (account, teams, named areas)

#### CC-05 — `summary-bot-person-id` hardcodé dans la config
**Sévérité : Faible**

Toujours présent. `BotInitializer` résout dynamiquement l'ID au démarrage, ce qui atténue le problème. Le champ de config reste un point de synchronisation manuel.

#### CC-06 — Emojis dans les logs de production
**Sévérité : Faible**

Emojis résiduels dans les logs des routes Camel. Gênant sur certains agrégateurs de logs (encodage). À nettoyer avant production.

#### CC-07 — `ddl-auto: update` en configuration principale ✅ RÉSOLU (P1-03)

Liquibase introduit avec changelog versionné. `ddl-auto` retiré.

---

## 3. Apache Camel — Best Practices

### ✅ Ce qui est bien fait

- `RouteBuilder` avec `configure()`
- `OrchestratorEndpoints` centralisé pour les URI `direct:`
- Circuit breaker Resilience4j sur **toutes** les opérations Unblu (P3-04)
- `routeId()` pour la traçabilité
- Séparation des routes par responsabilité
- Processors réduits au câblage Exchange (logique dans services Spring)

---

### ⚠️ Problèmes identifiés

#### CAM-01 — Logique métier dans les `Processor` ✅ RÉSOLU (P3-02)

`ConversationWorkflowService` regroupe toute la logique métier de l'orchestration. Les processors sont de purs câblages Exchange.

#### CAM-02 — Absence de gestion d'erreur structurée ✅ RÉSOLU (P1-04)

`WebhookEventRoute` définit des `onException()` pour `DataIntegrityViolationException` (doublon idempotent) et `Exception` générale (DLQ + log).

#### CAM-03 — Dépendance circulaire `application → infrastructure` ✅ RÉSOLU (P1-02)

La dépendance `provided` a été supprimée. L'architecture hexagonale est restaurée.

#### CAM-04 — Typage des corps de message Camel ✅ RÉSOLU (P2-01 + P3-03)

Les routes travaillent sur des types explicites :
- `UnbluWebhookPayload` pour les webhooks
- `ConversationOrchestrationState` pour le workflow principal
- DTOs de requête nommés (`PersonSearchRequest`, `DirectConversationRequest`, etc.)

#### CAM-05 — `ProducerTemplate` dans les services applicatifs
**Sévérité : Faible**

`ConversationOrchestratorService` utilise `ProducerTemplate.requestBody()`. Correct en pratique, mais couplé au contexte Camel en test. Acceptable pour un PoC.

#### CAM-06 — Route de résilience sous-utilisée ✅ RÉSOLU (P3-04)

Quatre routes résilientes couvrent toutes les opérations Unblu :

| Route résiliente | Fallback |
|-----------------|---------|
| `unblu-adapter-resilient` | `OFFLINE-PENDING` dans `ConversationOrchestrationState` |
| `unblu-search-persons-resilient` | Liste vide |
| `unblu-create-direct-conversation-resilient` | `ConversationData` avec id `OFFLINE-PENDING` |
| `unblu-add-summary-resilient` | Log silencieux (non critique) |

---

## 4. Persistance & Infrastructure

### ✅ Ce qui est bien fait

- Liquibase avec changelog versionné (`db.changelog-master.yaml`)
- `show-sql` et `format_sql` confinés au profil `local`
- Credentials DB externalisés dans `.env` (hors VCS)
- `.env.example` commité comme référence

---

### ⚠️ Problèmes identifiés

#### PERS-01 — Mapper sans `id` → bug de duplicate key ✅ RÉSOLU

`ConversationHistoryRepositoryAdapter.save()` charge l'entité existante et la mute en place.

#### PERS-02 — `orphanRemoval = true` avec mapper sans `id` ✅ RÉSOLU

Corrigé par la mutation en place de l'entité.

#### PERS-03 — Absence de migration de schéma ✅ RÉSOLU (P1-03)

Liquibase introduit. Structure :
```
unblu-infrastructure/src/main/resources/db/changelog/
  db.changelog-master.yaml
  migrations/
    001-initial-schema.sql
```

#### PERS-04 — `show-sql: true` en configuration principale ✅ RÉSOLU (P2-05)

Déplacé dans `application-local.yml` avec profil `local`.

---

## 5. Testabilité

### ⚠️ Problèmes identifiés

#### TEST-01 — Couverture de tests quasi nulle
**Sévérité : Haute — Dette assumée**

Un seul test existe (`UnbluProxyConfigTest`). Le Sprint 4 (tests) est volontairement différé avant la session de découverte API avec les développeurs.

**Priorité après la session :** les tests les plus utiles d'abord seront déterminés par ce qu'on aura découvert sur le comportement réel de l'API Unblu.

#### TEST-02 — Architecture difficilement testable ✅ PARTIELLEMENT RÉSOLU

L'architecture est maintenant testable :
- Les Processors sont de purs câblages (testables sans Spring)
- La logique métier est dans des services Spring injectables (mockables)
- Les ports sont des interfaces (remplaçables par des mocks)

Il manque uniquement les tests eux-mêmes.

---

## 6. Plan d'Action

### Sprints 1–3 : ✅ Complétés

| # | Action | Statut |
|---|--------|--------|
| P1-01 | Corriger `@Transactional` sur méthodes `private` | ✅ |
| P1-02 | Supprimer la dépendance `application → infrastructure` | ✅ |
| P1-03 | Introduire Liquibase | ✅ |
| P1-04 | Ajouter `onException()` dans `WebhookEventRoute` | ✅ |
| P2-01 | Désérialiser les payloads webhook en objets typés | ✅ |
| P2-02 | Décomposer `ConversationEventProcessor` | ✅ |
| P2-03 | Enrichir `ConversationHistory` avec invariants métier | ✅ |
| P2-04 | Uniformiser `port.secondary` → `port.out` | ✅ |
| P2-05 | `show-sql` et logs détaillés en profil `local` | ✅ |
| P3-01 | Extraire `UnbluPersonService`, `UnbluConversationService`, etc. | ✅ |
| P3-02 | Déplacer la logique métier des Processors vers des services | ✅ |
| P3-03 | Séparer `ConversationContext` de `ConversationOrchestrationState` | ✅ |
| P3-04 | Activer le circuit breaker sur toutes les opérations Unblu | ✅ |

### Sprint 4 — Tests (différé, après session API)

| # | Action | Effort | Priorité |
|---|--------|--------|---------|
| P4-01 | Tests unitaires `ConversationHistoryMapper`, `ConversationMapper` | 3h | Haute |
| P4-02 | Tests unitaires `ConversationHistoryService` (invariants domaine) | 2h | Haute |
| P4-03 | Tests d'intégration des use cases avec Camel Test | 6h | Moyenne |
| P4-04 | Tests de contrat sur les ports secondaires (mock vs real) | 4h | Faible |

> **Recommandation :** après la session, commencer par P4-01 et P4-02 — ce sont les plus rapides à écrire et protègent les règles métier récemment ajoutées sur `ConversationHistory`.

---

## 7. Recommandations Architecturales Long Terme

Cette section s'adresse à une éventuelle montée en charge du PoC vers un système de production. Les points ci-dessous ne sont **pas urgents** pour la session de découverte.

---

### 7.1 Idempotence des webhooks

**Priorité : Haute si production**

Les webhooks Unblu peuvent être re-livrés en cas d'échec réseau. Le `onException(DataIntegrityViolationException)` actuel est un garde-fou, mais il repose sur une contrainte de base de données — ce n'est pas une gestion d'idempotence explicite.

#### Pourquoi le mécanisme actuel est insuffisant

Aujourd'hui, un doublon provoque une `DataIntegrityViolationException` qui est attrapée et loguée. C'est défensif : on laisse la base "exploser" puis on rattrape. L'idempotence explicite consiste à vérifier **avant** tout traitement, de façon intentionnelle et lisible.

#### Implémentation complète dans l'architecture hexagonale

**Étape 1 — Port secondaire dans le domaine**

```java
// unblu-domain/.../port/out/ProcessedEventRepository.java
public interface ProcessedEventRepository {
    boolean alreadyProcessed(String eventId);
    void markAsProcessed(String eventId);
}
```

Ce port est dans `domain.port.out` comme `ConversationHistoryRepository`. Le domaine définit le contrat, l'infrastructure l'implémente.

**Étape 2 — Entité JPA dans l'infrastructure**

```java
// unblu-infrastructure/.../persistence/entity/ProcessedEventEntity.java
@Entity
@Table(name = "webhook_processed_events")
public class ProcessedEventEntity {
    @Id
    private String eventId;
    private Instant processedAt;
}
```

**Étape 3 — Migration Liquibase**

```sql
-- unblu-infrastructure/.../db/changelog/migrations/002-webhook-idempotency.sql
CREATE TABLE webhook_processed_events (
    event_id     VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);
```

**Étape 4 — Adaptateur dans l'infrastructure**

```java
// unblu-infrastructure/.../adapter/idempotency/ProcessedEventRepositoryAdapter.java
@Component
@RequiredArgsConstructor
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    private final ProcessedEventJpaRepository jpaRepository;

    @Override
    public boolean alreadyProcessed(String eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    @Transactional
    public void markAsProcessed(String eventId) {
        jpaRepository.save(new ProcessedEventEntity(eventId, Instant.now()));
    }
}
```

**Étape 5 — Utilisation dans `WebhookEventRoute` (application)**

`WebhookEventRoute` injecte le port `ProcessedEventRepository` (interface domaine, pas l'adaptateur) :

```java
// unblu-application/.../route/webhook/WebhookEventRoute.java
@Component
@RequiredArgsConstructor
public class WebhookEventRoute extends RouteBuilder {

    private final ProcessedEventRepository processedEventRepository;

    @Override
    public void configure() {
        from("direct:webhook-event")
            .routeId("webhook-event-router")

            // Idempotence : court-circuit si déjà traité
            .process(exchange -> {
                String eventId = exchange.getIn()
                    .getHeader("X-Unblu-Event-Id", String.class);
                if (eventId != null && processedEventRepository.alreadyProcessed(eventId)) {
                    log.info("Webhook event already processed, skipping: {}", eventId);
                    exchange.setRouteStop(true);
                }
            })

            // Traitement normal...
            .choice()
                .when(/* eventType */)...
            .end()

            // Marquer comme traité après succès
            .process(exchange -> {
                String eventId = exchange.getIn()
                    .getHeader("X-Unblu-Event-Id", String.class);
                if (eventId != null) {
                    processedEventRepository.markAsProcessed(eventId);
                }
            });
    }
}
```

#### Résumé de la chaîne

```
WebhookReceiverRoute          ← reçoit le HTTP POST Unblu
    ↓ header X-Unblu-Event-Id
WebhookEventRoute             ← vérifie ProcessedEventRepository (port domaine)
    ↓ si nouveau
ConversationEventProcessor    ← dispatch vers le bon handler
    ↓
ConversationHistoryService    ← logique métier @Transactional
    ↓
ProcessedEventRepositoryAdapter ← marque l'eventId en base (adaptateur infra)
```

`idempotencyStore` dans le snippet initial était un raccourci pour `ProcessedEventRepository` — le port secondaire qui suit exactement le même pattern que `ConversationHistoryRepository` déjà en place.

---

### 7.2 ConversationHistory comme Agrégat DDD complet

**Priorité : Moyenne si production**

`ConversationHistory` a maintenant des invariants métier (`end()`, `recordMessage()`) — c'est une bonne fondation. Pour aller vers un Agrégat DDD complet, il manque les **événements de domaine** :

```
ConversationHistory (Agrégat)
├── ConversationId (Value Object typé, pas un String)
├── Topic          (Value Object typé, pas un String)
├── ConversationState (enum : CREATED → ACTIVE → ENDED)
├── end()           → publie ConversationEndedEvent
├── recordMessage() → publie ConversationMessageAddedEvent
└── List<DomainEvent> uncommittedEvents  ← ce qui manque
```

**Bénéfice concret :** découpler la persistence et les notifications de la logique métier. Aujourd'hui `ConversationHistoryService` doit à la fois valider les règles ET appeler le repository. Avec les événements de domaine :

```java
// Dans ConversationHistoryService
ConversationHistory history = repository.findById(id);
history.recordMessage(text, sender, time);      // règle métier
repository.save(history);                        // persistence
eventPublisher.publish(history.uncommittedEvents()); // notifications
```

**Quand l'introduire :** si un deuxième consommateur des événements de conversation apparaît (analytics, notifications temps réel, audit légal).

---

### 7.3 Bounded Contexts explicites

**Priorité : Faible — vision long terme**

Le projet mélange actuellement deux sous-domaines qui grandissent indépendamment :

| Sous-domaine | Responsabilité | Modèle clé |
|-------------|---------------|-----------|
| **Orchestration** | Démarrer une conversation, router vers le bon agent | `ConversationContext`, `ChatRoutingDecision` |
| **Historique** | Enregistrer et requêter les événements de conversation | `ConversationHistory`, `ConversationEventHistory` |

Ces deux contextes partagent aujourd'hui l'identifiant `conversationId` comme référence croisée — c'est sain. Mais leurs modèles divergent déjà (`ConversationHistory` a des événements, `ConversationContext` a un routingDecision).

**Signal d'alarme :** si on commence à enrichir `ConversationHistory` avec des données de routage, ou `ConversationContext` avec l'historique, c'est le moment de séparer en deux modules Maven distincts avec des packages `domain` indépendants.

---

### 7.4 Observabilité structurée

**Priorité : Moyenne si production**

Les logs actuels sont lisibles mais non structurés. En production, les logs doivent être interrogeables :

```java
// Actuel — difficile à filtrer en production
log.info("Conversation Event Received - Type: {}", eventType);

// Recommandé — champs structurés pour ELK/Datadog
log.info("Webhook event received",
    kv("eventType", eventType),
    kv("conversationId", conversationId),
    kv("accountId", accountId)
);
```

**Ce qu'il faut :** ajouter `logstash-logback-encoder` et configurer un appender JSON. Effort : 2h. Impact en production : majeur.

---

### 7.5 Gestion du cycle de vie des tokens Unblu

**Priorité : Haute si production**

Le PoC utilise Basic Auth (username/password) pour toutes les requêtes Unblu. En production :
- Les credentials doivent être stockés dans un vault (HashiCorp Vault, AWS Secrets Manager)
- Si Unblu supporte OAuth2, préférer des tokens à courte durée de vie avec refresh automatique
- Le `BotInitializer` au démarrage échoue silencieusement si les credentials sont invalides — il faudrait un health check Actuator dédié

---

## Conclusion

Le projet PocUnblu est dans un **bon état pour une session de découverte API**. Les 3 premiers sprints ont traité toutes les dettes architecturales majeures : l'hexagonale est propre, les objets domaine ont des invariants, les webhooks sont typés, le circuit breaker couvre toutes les opérations Unblu.

**Ce qui reste à faire avant de considérer ce code "production-ready" :**

| Priorité | Action | Effort estimé |
|---------|--------|--------------|
| 🔴 Haute | Tests unitaires + intégration (Sprint 4) | 15h |
| 🔴 Haute | Idempotence des webhooks (§7.1) | 4h |
| 🔴 Haute | Gestion des credentials via vault (§7.5) | variable |
| 🟡 Moyenne | Logs structurés JSON (§7.4) | 2h |
| 🟡 Moyenne | Événements de domaine sur `ConversationHistory` (§7.2) | 6h |
| 🟢 Faible | Bounded Contexts séparés (§7.3) | — |
| 🟢 Faible | Nettoyer les emojis dans les logs (CC-06) | 30min |
