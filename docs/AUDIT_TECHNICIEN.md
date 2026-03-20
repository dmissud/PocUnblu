# Audit Technique — PocUnblu
**Date :** 2026-03-20
**Auteur :** Tech Lead Senior
**Périmètre :** Architecture hexagonale, DDD, Clean Code, Apache Camel

---

## Résumé Exécutif

Le projet présente une **base architecturale solide** avec une structure hexagonale bien organisée et un découpage modulaire Maven cohérent. Le proof-of-concept atteint son objectif d'intégration Unblu. Cependant, plusieurs écarts aux principes DDD, Clean Code et bonnes pratiques Camel sont identifiés — certains sont des compromis de PoC acceptables, d'autres méritent correction avant toute montée en charge.

**Score global : 6.5 / 10**

| Domaine          | Score |
|------------------|-------|
| Architecture DDD | 7/10  |
| Clean Code       | 6/10  |
| Camel Best Practices | 5.5/10 |
| Persistance      | 5/10  |
| Testabilité      | 3/10  |

---

## 1. Architecture & DDD

### ✅ Ce qui est bien fait

**Séparation des modules Maven**
Le découpage `domain → application → infrastructure → exposition → configuration` respecte fidèlement la dépendance hexagonale. Le domaine ne dépend de rien d'externe.

**Value Objects avec Records Java**
`CustomerProfile`, `ChatRoutingDecision`, `PersonInfo`, `TeamInfo` sont des records immuables avec validation dans le constructeur compact. C'est exactement ce qu'on attend de Value Objects DDD.

```java
// Bien : validation dans le constructeur compact du record
public record ChatRoutingDecision(boolean isAuthorized, String unbluAssignedGroupId, String routingReason) {
    public ChatRoutingDecision {
        if (isAuthorized) {
            Objects.requireNonNull(unbluAssignedGroupId, "unbluAssignedGroupId must be provided for authorized chats");
        }
    }
}
```

**Ports & Adapters clairement définis**
`UnbluPort`, `ErpPort`, `RuleEnginePort`, `ConversationHistoryRepository` sont des interfaces propres dans le domaine. Les adaptateurs dans `infrastructure` implémentent ces interfaces sans que le domaine en sache rien.

**Commands comme objets immuables**
`StartConversationCommand` et `StartDirectConversationCommand` sont des records avec validation — le bon pattern pour les commandes CQRS.

---

### ⚠️ Problèmes identifiés

#### DDD-01 — `ConversationHistory` est une entité anémique
**Sévérité : Haute**

`ConversationHistory` est annotée `@Data` (Lombok) et exposée comme un simple DTO mutable. Elle devrait être une **entité riche** avec comportement métier. La logique de tri, de validation de l'état, de règles sur les transitions est absente.

```java
// Actuel : entité anémique
@Data @Builder
public class ConversationHistory {
    private String conversationId;
    private String topic;
    // ...
    public void addEvent(ConversationEventHistory event) {
        this.events.add(event); // aucune règle métier
    }
}
```

```java
// Recommandé : entité avec invariants
public class ConversationHistory {
    // ...
    public void addMessage(String messageText, String senderPersonId, String senderDisplayName, Instant time) {
        if (this.endedAt != null) {
            throw new IllegalStateException("Cannot add message to an ended conversation");
        }
        this.events.add(ConversationEventHistory.message(messageText, senderPersonId, senderDisplayName, time));
    }

    public void end(Instant endedAt) {
        if (this.endedAt != null) throw new IllegalStateException("Conversation already ended");
        this.endedAt = endedAt;
        this.events.add(ConversationEventHistory.ended(endedAt));
    }
}
```

#### DDD-02 — `ConversationContext` est un objet pivot non-DDD
**Sévérité : Moyenne**

`ConversationContext` sert d'objet de transport Camel (pattern Pivot Object). C'est un choix pragmatique pour Camel, mais il cumule plusieurs responsabilités : état de l'orchestration, données client, décision de routage, résultat Unblu. Il ne représente pas un concept métier cohérent.

**Recommandation :** Séparer en deux :
- `ConversationOrchestrationState` (objet Camel interne à `application`)
- `ConversationContext` (concept domaine pur si pertinent)

#### DDD-03 — Ports secondaires dans le mauvais package
**Sévérité : Faible**

`ErpPort`, `RuleEnginePort`, `ConversationSummaryPort` sont dans `domain.port.secondary` alors que `ConversationHistoryRepository` est dans `domain.port.out`. Incohérence de nommage qui signale une hésitation sur la convention.

**Recommandation :** Uniformiser en `domain.port.out` pour tous les ports secondaires.

#### DDD-04 — Absence de Bounded Context explicite
**Sévérité : Faible (PoC)**

Le projet mélange sans séparation le sous-domaine "orchestration de conversation" et le sous-domaine "historique/audit". Dans un système plus grand, ces deux contextes auraient leurs propres modèles.

---

## 2. Clean Code

### ✅ Ce qui est bien fait

- Nommage explicite et en français cohérent pour les logs
- Constantes bien externalisées dans `OrchestratorEndpoints`
- Records Java pour les Value Objects (zéro boilerplate)
- `@RequiredArgsConstructor` + injection par constructeur (testable)
- `GlobalExceptionHandler` centralisé

---

### ⚠️ Problèmes identifiés

#### CC-01 — `ConversationEventProcessor` : classe God (322 lignes)
**Sévérité : Haute**

`ConversationEventProcessor` fait tout : routing des events, extraction des champs depuis des `Map<String,Object>`, logique de persistence, gestion des participants. Elle viole le **Single Responsibility Principle**.

```
ConversationEventProcessor (322 lignes)
├── process()              → dispatch
├── handleConversationCreated()  → extraction + persistence
├── handleNewMessage()           → extraction + enrichissement + persistence
├── handleConversationEnded()    → extraction + persistence
├── extractConversationId()      → utilitaire extraction Map
├── extractEventType()           → utilitaire extraction Map
└── logPayloadFields()           → logging
```

**Recommandation :** Extraire :
- `WebhookPayloadExtractor` — extraction des champs depuis `Map<String,Object>`
- `ConversationHistoryService` (application service) — logique métier de persistence
- `ConversationEventProcessor` — uniquement le dispatch vers les handlers

#### CC-02 — Parsing de `Map<String, Object>` répété partout
**Sévérité : Haute**

Le code extrait les champs du payload webhook via des accès `Map` répétés avec casts non typés dans `ConversationEventProcessor`, `WebhookEventTypeExtractor`, `WebhookReceiverRoute`. C'est fragile, non typé, et dupliqué.

```java
// Répété dans 3 classes différentes
Object conversationMessageObj = payload.get(FIELD_CONVERSATION_MESSAGE);
if (conversationMessageObj instanceof Map) {
    @SuppressWarnings("unchecked")
    Map<String, Object> conversationMessageData = (Map<String, Object>) conversationMessageObj;
    conversationId = (String) conversationMessageData.get(FIELD_CONVERSATION_ID);
}
```

**Recommandation :** Désérialiser le payload en objets typés dès la réception dans `WebhookReceiverRoute` avec Jackson, puis router des objets typés `ConversationCreatedEvent`, `ConversationNewMessageEvent`, etc.

#### CC-03 — `@Transactional` sur méthode `private`
**Sévérité : Haute**

```java
@Transactional
private void handleConversationCreated(Map<String, Object> payload) { ... }
```

Spring AOP ne peut pas intercepter les méthodes `private`. L'annotation `@Transactional` est **silencieusement ignorée**. La transaction ouverte par l'appelant (si présente) est utilisée, sinon il n'y en a pas du tout.

**Recommandation :** Rendre les méthodes `package-private` ou `protected`, ou extraire dans un service séparé annoté `@Transactional` au niveau classe.

#### CC-04 — `UnbluService` : classe trop longue (670 lignes)
**Sévérité : Moyenne**

`UnbluService` regroupe toutes les opérations Unblu (personnes, conversations, équipes, bots, named areas, messages). Ce n'est pas une violation critique ici car c'est une façade sur le SDK externe, mais elle mérite une décomposition.

**Recommandation :** Extraire par domaine fonctionnel : `UnbluPersonService`, `UnbluConversationService`, `UnbluBotService`.

#### CC-05 — `summary-bot-person-id` hardcodé dans la config
**Sévérité : Moyenne**

```yaml
unblu:
  api:
    summary-bot-person-id: I2mWhL8AREW88xHy3BmdcA
```

Un identifiant technique Unblu ne devrait pas être une valeur de configuration statique. Il devrait être résolu dynamiquement au démarrage (comme `BotInitializer` commence à le faire) ou via une propriété nommée.

#### CC-06 — Emojis dans les logs de production
**Sévérité : Faible**

```java
log.info("🔔 Conversation Event Received!");
log.info("🎉 NEW CONVERSATION CREATED!");
log.info("✅ Conversation history saved to database with ID: {}", ...);
```

Les emojis dans les logs posent des problèmes d'encodage sur certains systèmes (Windows, certains agrégateurs de logs). Utiliser des niveaux de log appropriés et des marqueurs structurés à la place.

#### CC-07 — `ddl-auto: update` en configuration principale
**Sévérité : Moyenne**

`spring.jpa.hibernate.ddl-auto: update` ne doit jamais être utilisé en production. Il peut corrompre le schéma silencieusement. À remplacer par Liquibase ou Flyway dès la sortie du PoC.

---

## 3. Apache Camel — Best Practices

### ✅ Ce qui est bien fait

- Utilisation de `RouteBuilder` avec `configure()`
- `OrchestratorEndpoints` centralisé pour les URI `direct:`
- Circuit breaker avec `Resilience4j` via `circuitBreaker()`
- Logs avec `routeId()` pour la traçabilité
- Séparation des routes par responsabilité

---

### ⚠️ Problèmes identifiés

#### CAM-01 — Logique métier dans les `Processor` au lieu des `RouteBuilder`
**Sévérité : Haute**

`ConversationEventProcessor` implémente `Processor` et contient toute la logique métier. Le rôle d'un `Processor` Camel est de transformer/enrichir le message, pas d'implémenter des use cases complets.

**Recommandation :** Les `Processor` doivent rester légers (transformer, router). La logique métier doit être dans des services Spring appelés depuis la route :

```java
// Recommandé dans le RouteBuilder
.process(exchange -> {
    ConversationCreatedPayload payload = exchange.getIn().getBody(ConversationCreatedPayload.class);
    conversationHistoryService.onConversationCreated(payload);
})
```

#### CAM-02 — Absence de gestion d'erreur structurée dans les routes
**Sévérité : Haute**

Aucune route ne définit de `onException()`. Les erreurs sont remontées par le `DefaultErrorHandler` de Camel (log + re-delivery par défaut). En production, un webhook raté doit être rejoué ou mis en Dead Letter Channel.

```java
// Recommandé dans WebhookEventRoute
onException(DataIntegrityViolationException.class)
    .handled(true)
    .log(LoggingLevel.ERROR, "Duplicate conversation event ignored: ${exception.message}")
    .end();

onException(Exception.class)
    .maximumRedeliveries(3)
    .redeliveryDelay(1000)
    .to("log:dead-letter?level=ERROR")
    .handled(true)
    .end();
```

#### CAM-03 — Dependency circulaire `application → infrastructure`
**Sévérité : Haute**

Dans le `pom.xml` de `unblu-application` :
```xml
<dependency>
    <groupId>org.dbs.poc</groupId>
    <artifactId>unblu-infrastructure</artifactId>
    <scope>provided</scope>
</dependency>
```

L'application dépend de l'infrastructure (même en `provided`) — c'est une **violation de l'architecture hexagonale**. La couche application ne doit avoir aucune dépendance vers l'infrastructure.

**Cause probable :** utilisation de classes infrastructure dans les routes Camel de `application`. À investiguer et corriger.

#### CAM-04 — Pas de typage des corps de message Camel
**Sévérité : Moyenne**

Les routes Camel passent des `Map<String, Object>`, `String`, `ConversationContext` sans documentation ni contrat. En cas d'évolution, il est impossible de savoir quel type est attendu à chaque étape sans lire tout le code.

**Recommandation :** Documenter avec des commentaires de type dans le `configure()`, ou créer des DTOs de pipeline explicites.

#### CAM-05 — `ProducerTemplate` dans les services applicatifs
**Sévérité : Moyenne**

`ConversationOrchestratorService` et `DirectConversationService` utilisent `ProducerTemplate.requestBody()` pour appeler des routes Camel. C'est correct mais cela rend ces services difficiles à tester unitairement (il faut un contexte Camel complet).

**Recommandation :** Extraire une interface `ConversationOrchestrator` que `ProducerTemplate` implémente, ou utiliser `FluentProducerTemplate` qui est plus testable.

#### CAM-06 — Route de résilience sous-utilisée
**Sévérité : Faible**

`UnbluResilientRoute` (`direct:unblu-adapter-resilient`) est définie mais n'est pas utilisée par toutes les opérations Unblu. `StartDirectConversationRoute` appelle directement `unbluCamelAdapterPort.searchPersons()` sans passer par le circuit breaker.

---

## 4. Persistance & Infrastructure

### ⚠️ Problèmes identifiés

#### PERS-01 — Mapper sans `id` → bug de duplicate key (corrigé)
Ce bug a été identifié et corrigé en session. Le `ConversationHistoryMapper.toEntity()` créait des entités sans `id`, forçant un INSERT au lieu d'un UPDATE.

**Correctif appliqué :** `ConversationHistoryRepositoryAdapter.save()` charge l'entité existante et la mute en place.

**Recommandation de fond :** Cette correction est fonctionnelle mais contourne un problème de design. Idéalement, le domaine devrait ne pas utiliser un mapper qui perd l'identité technique. Deux options :
1. Le domaine porte l'`id` technique (compromis pragmatique)
2. L'adaptateur fait le merge JPA directement sans passer par le domain mapper pour les updates

#### PERS-02 — `orphanRemoval = true` avec mapper sans `id` (corrigé)
`orphanRemoval = true` sur les collections JPA supprimait les anciennes lignes lors d'un save. Corrigé par mutation en place de l'entité existante.

#### PERS-03 — Absence de migration de schéma
**Sévérité : Haute**

`ddl-auto: update` ne maintient aucun historique des changements. L'ajout du champ `topic` (fait en session) ne génère aucune migration versionnée. À la prochaine suppression/recréation de la DB, l'historique des migrations est perdu.

**Recommandation :** Introduire **Liquibase** avec un changelog initial généré depuis l'état actuel.

#### PERS-04 — `show-sql: true` en configuration principale
**Sévérité : Faible**

`spring.jpa.show-sql: true` pollue les logs en production. À déplacer dans un profil `dev` uniquement.

---

## 5. Testabilité

### ⚠️ Problèmes identifiés

#### TEST-01 — Couverture de tests quasi nulle
**Sévérité : Haute**

Un seul test existe : `UnbluProxyConfigTest`. Pas de test pour :
- Les use cases (services applicatifs)
- Les mappers
- La logique d'extraction des payloads webhook
- Les règles métier de `ConversationHistory`

#### TEST-02 — Architecture difficilement testable
**Sévérité : Haute**

La logique dans `ConversationEventProcessor` (300+ lignes, accès direct au repository, parsing de Map) est impossible à tester unitairement sans Spring context complet.

---

## 6. Plan d'Action

### Priorité 1 — Corrections bloquantes (Sprint 1)

| # | Action | Fichiers | Effort |
|---|--------|----------|--------|
| P1-01 | Corriger `@Transactional` sur méthodes `private` | `ConversationEventProcessor` | 1h |
| P1-02 | Supprimer la dépendance `application → infrastructure` du POM | `unblu-application/pom.xml` | 2h |
| P1-03 | Introduire Liquibase avec migration initiale | `unblu-infrastructure`, `unblu-configuration` | 3h |
| P1-04 | Ajouter `onException()` dans `WebhookEventRoute` | `WebhookEventRoute` | 2h |

### Priorité 2 — Qualité de code (Sprint 2)

| # | Action | Fichiers | Effort |
|---|--------|----------|--------|
| P2-01 | Désérialiser les payloads webhook en objets typés dès `WebhookReceiverRoute` | `WebhookReceiverRoute`, nouveaux DTOs | 4h |
| P2-02 | Décomposer `ConversationEventProcessor` en `WebhookPayloadExtractor` + handlers | `application/route/processor/` | 4h |
| P2-03 | Enrichir `ConversationHistory` avec invariants métier | `domain/model/history/` | 3h |
| P2-04 | Uniformiser les packages `port.secondary` → `port.out` | `domain/port/` | 1h |
| P2-05 | Passer `show-sql` et logs détaillés en profil `dev` | `application.yml` | 30min |

### Priorité 3 — Qualité architecturale (Sprint 3)

| # | Action | Fichiers | Effort |
|---|--------|----------|--------|
| P3-01 | Extraire `UnbluPersonService`, `UnbluConversationService`, `UnbluBotService` | `UnbluService` | 3h |
| P3-02 | Déplacer la logique métier des `Processor` vers des services Spring | `application/service/` | 4h |
| P3-03 | Séparer `ConversationContext` (objet Camel) du domaine | `domain/model/`, `application/` | 3h |
| P3-04 | Activer le circuit breaker sur toutes les opérations Unblu | `StartDirectConversationRoute` | 2h |

### Priorité 4 — Tests (Sprint 4)

| # | Action | Effort |
|---|--------|--------|
| P4-01 | Tests unitaires des mappers (`ConversationHistoryMapper`, `ConversationMapper`) | 3h |
| P4-02 | Tests unitaires des extracteurs de payload webhook | 2h |
| P4-03 | Tests d'intégration des use cases avec Camel Test | 6h |
| P4-04 | Tests de contrat sur les ports secondaires (mock vs real) | 4h |

---

## 7. Recommandations Architecturales Long Terme

### Vers un vrai DDD

Pour une application de production, les conversations ont un cycle de vie riche (CREATED → ACTIVE → ENDED) qui mérite un **Aggregate** au sens DDD :

```
ConversationAggregate
├── ConversationId (Value Object)
├── Topic (Value Object)
├── ConversationState (CREATED | ACTIVE | ENDED)
├── addMessage() → génère ConversationMessageAddedEvent
├── end() → génère ConversationEndedEvent
└── List<DomainEvent> uncommittedEvents
```

Les événements de domaine (`ConversationMessageAddedEvent`) déclencheraient la persistence et les notifications via un `DomainEventPublisher`, découplant complètement la logique métier de l'infrastructure.

### Webhook Idempotency

Les webhooks Unblu peuvent être re-livrés. Il faut un mécanisme d'idempotence :
- Stocker l'`eventId` de chaque webhook reçu
- Rejeter les doublons avant traitement

### Liquibase Strategy

```
unblu-infrastructure/
  src/main/resources/
    db/
      changelog/
        db.changelog-master.xml
        migrations/
          001-initial-schema.sql
          002-add-topic-column.sql   ← la migration du jour
```

---

## Conclusion

Le projet PocUnblu démontre une bonne maîtrise des concepts hexagonaux et une intégration fonctionnelle d'Unblu via Apache Camel. Les problèmes identifiés sont typiques d'un PoC qui grandit : la logique métier commence à s'accumuler dans des composants techniques, les tests manquent, et les contrats entre couches s'effritent.

**Les 3 actions les plus importantes avant de considérer ce code "production-ready" :**

1. **Liquibase** — sans historique de schéma, chaque déploiement est un risque
2. **Désérialisation typée des webhooks** — les `Map<String,Object>` parsées à la main sont la principale source de bugs actuels et futurs
3. **Tests** — l'absence totale de tests rend tout refactoring périlleux

Le code est **propre dans son intention**, les patterns sont **correctement identifiés** et **partiellement bien appliqués**. La dette technique accumulée est **gérable** avec un plan d'action discipliné sur 4 sprints.
