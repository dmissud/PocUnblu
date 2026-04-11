# Gestion des Conversations Unblu — Orchestration

Ce document couvre les opérations de gestion des conversations dans le bloc Orchestration :
création standard, directe (1-à-1), et ajout de résumé via bot.

> La synchronisation des conversations (listing + persistance) est dans le bloc Integration :
> [`integration/unblu-adapter-doc/sync-conversations.md`](../../integration/unblu-adapter-doc/sync-conversations.md)

## Service Principal : `UnbluConversationService`

Ce service utilise le SDK Unblu via `ConversationsApi` et `BotsApi`. Toutes les `ApiException`
levées par le SDK sont interceptées et retransformées en `UnbluApiException`.

---

## Scénarios d'Usage

### 1. Création d'une conversation standard (vers une équipe)

Cas nominal : un visiteur demande un chat ; le système le route vers une équipe d'agents.

```mermaid
sequenceDiagram
    participant App as Application Core
    participant Port as UnbluCamelAdapterPort
    participant CB as UnbluResilientRoute (CB)
    participant Adapter as UnbluCamelAdapter
    participant PersonSvc as UnbluPersonService
    participant ConvSvc as UnbluConversationService
    participant Unblu as Unblu REST API

    App->>Port: createConversation(context)
    Port->>CB: requestBody(DIRECT_UNBLU_ADAPTER_RESILIENT, state)
    CB->>Adapter: process(exchange)
    Adapter->>PersonSvc: getPersonBySource(VIRTUAL, clientId)
    PersonSvc->>Unblu: GET /v4/persons/getBySource
    Unblu-->>PersonSvc: PersonData
    Adapter->>ConvSvc: createConversation(creationData)
    ConvSvc->>Unblu: POST /v4/conversations/create
    Unblu-->>ConvSvc: ConversationData (id, joinUrl)
    ConvSvc-->>Adapter: ConversationData
    Adapter-->>CB: Updated State
    CB-->>Port: Updated State
    Port-->>App: UnbluConversationInfo(id, joinUrl)
```

- **Fallback circuit breaker** : retourne un `ConversationOrchestrationState` avec l'ID `OFFLINE-PENDING`.

---

### 2. Création d'une conversation directe (1-à-1)

Met directement en relation un visiteur et un agent connu.

- **Validation métier** : si l'`agentPerson` fourni n'est pas de type `AGENT`, une `UnbluApiException(400)` est levée avant même d'appeler l'API.
- **Participants** : le visiteur est ajouté avec le rôle `CONTEXT_PERSON`, l'agent avec `ASSIGNED_AGENT`.
- **Endpoint Unblu** : `POST /v4/conversations/create` (même endpoint, paramètres différents).
- **Fallback** : retourne un `ConversationData` avec l'ID `OFFLINE-PENDING`.

---

### 3. Ajout d'un résumé via Bot

Utilisé en fin de parcours pour laisser une trace textuelle dans la conversation.

- **Prérequis** : `unblu.api.summary-bot-person-id` doit être configuré. Si absent → warning loggé, opération silencieusement ignorée.
- **Processus** :
  1. `POST /v4/conversations/{id}/addParticipant` — ajout du bot en mode `hidden: true`.
  2. `POST /v4/bots/sendMessage` — envoi du résumé sous forme de `TextPostMessageData`.
- **Fallback** : résumé ignoré silencieusement (non critique).

---

## Endpoints Unblu utilisés

| Méthode | Endpoint | Utilisation |
|---------|----------|-------------|
| `POST` | `/v4/conversations/create` | Création conversation standard et directe |
| `POST` | `/v4/conversations/{id}/addParticipant` | Ajout du bot résumé |
| `POST` | `/v4/bots/sendMessage` | Envoi du message de résumé |

---

## Gestion des Erreurs

| Code HTTP | Cause | Comportement |
|-----------|-------|-------------|
| `400` | Agent fourni n'est pas de type `AGENT` | `UnbluApiException(400)` levée avant l'appel API |
| `403` | Droits insuffisants sur la clé API | `UnbluApiException(403)` avec message explicite |
| `404` | Ressource inexistante (personne, conversation) | `UnbluApiException(404)` propagée |
| Autres | Erreur technique SDK | `UnbluApiException` avec code HTTP d'origine |
