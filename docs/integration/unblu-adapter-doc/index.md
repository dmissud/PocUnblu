# Usages Unblu — Bloc Integration

Ce guide couvre l'utilisation de l'API Unblu dans le bloc Integration :
synchronisation des conversations, enrichissement, et dialogue bot.

## Port secondaire : `IntegrationUnbluPort`

Défini dans `integration-domain`, implémenté par `IntegrationUnbluAdapter`
(`integration-infrastructure`). Il couvre le sous-ensemble d'opérations Unblu
nécessaires au pipeline d'intégration.

```java
interface IntegrationUnbluPort {
    UnbluConversationInfo createConversation(ConversationCreationRequest request);
    List<UnbluConversationSummary> listAllConversations();
    List<UnbluMessageData> fetchConversationMessages(String conversationId);
    List<UnbluParticipantData> fetchConversationParticipants(String conversationId);
}
```

| Opération | Utilisation |
|-----------|-------------|
| `listAllConversations()` | Synchronisation à la demande : scan Unblu → persistance DB |
| `fetchConversationMessages()` | Enrichissement d'une conversation existante en base |
| `fetchConversationParticipants()` | Enrichissement d'une conversation existante en base |
| `createConversation()` | Création de conversation depuis le microservice `livekit` |

### Résilience

`IntegrationUnbluAdapter` est annoté `@CircuitBreaker(name = "unblu")` et
`@Retry(name = "unblu")` via Resilience4j (annotations Spring AOP, sans route Camel).

---

## Dialogue bot : `PocBotDialogService` (livekit)

Le microservice `livekit` utilise directement le SDK Unblu (sans passer par
`IntegrationUnbluPort`) pour piloter le dialogue du bot :

| API Unblu | Utilisation |
|-----------|-------------|
| `ConversationsApi.setRecipient()` | Positionne le namedArea sur la conversation |
| `BotsApi.sendTextMessage()` | Envoie le message de bienvenue / résumé |
| `BotsApi.handOffToAgent()` | Transfert de la conversation vers un agent humain |

Ces appels sont exécutés de façon **asynchrone** (`CompletableFuture.runAsync()`) afin de
retourner l'acquittement à Unblu-Hookshot immédiatement.

---

## Documentation détaillée

| Document | Périmètre |
|----------|-----------|
| [Synchronisation des Conversations](./sync-conversations.md) | Scan Unblu → persistance en base, idempotence, rapport de résultat |
| [Consultation IHM — Historique](./history-ihm.md) | API de consultation paginée + triée, interface Angular |

> Les opérations de création de conversation, gestion des personnes, équipes et bots
> sont documentées dans le bloc Orchestration :
> [`orchestration/unblu-adapter-doc/index.md`](../../orchestration/unblu-adapter-doc/index.md)
