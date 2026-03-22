# 🗨️ Gestion des Conversations Unblu

Ce document détaille tout ce qui touche au cycle de vie des conversations Unblu : création, liaison 1-à-1, ajout de résumé, et listing pour synchronisation.

## 🧱 Service Principal : `UnbluConversationService`

Ce service utilise le SDK Unblu via `ConversationsApi` et `BotsApi`. Toutes les `ApiException` levées par le SDK sont interceptées et retransformées en `UnbluApiException`.

---

## 🏃 Scénarios d'Usage

### 1. Création d'une conversation standard (vers une équipe)

Cas nominal : un visiteur demande un chat ; le système le route vers une équipe d'agents.

**Flux de séquence :**

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

### 4. Listing de toutes les conversations (`listAllConversations`)

Récupère la liste complète des conversations présentes dans Unblu pour alimenter la synchronisation en base.

**Flux de séquence :**

```mermaid
sequenceDiagram
    participant App as SyncConversationsService
    participant Port as UnbluCamelAdapterPort
    participant CB as UnbluResilientRoute (CB)
    participant Adapter as UnbluCamelAdapter
    participant ConvSvc as UnbluConversationService
    participant Unblu as Unblu REST API

    App->>Port: listAllConversations()
    Port->>CB: requestBody(DIRECT_UNBLU_LIST_CONVERSATIONS_RESILIENT, null)
    CB->>Adapter: process(exchange)
    Adapter->>ConvSvc: listAllConversations()
    ConvSvc->>Unblu: POST /v4/conversations/search (ConversationQuery vide)
    Unblu-->>ConvSvc: ConversationResult (List<ConversationData>)
    Note right of ConvSvc: Mapping ConversationData → UnbluConversationSummary\ncreationTimestamp → Instant, endTimestamp → Instant (nullable)
    ConvSvc-->>Port: List<UnbluConversationSummary>
    Port-->>App: List<UnbluConversationSummary>
```

**Modèle de sortie `UnbluConversationSummary` :**

| Champ | Type | Source Unblu |
|-------|------|-------------|
| `id` | `String` | `ConversationData.getId()` |
| `topic` | `String` | `ConversationData.getTopic()` |
| `state` | `String` | `ConversationData.getState().name()` |
| `createdAt` | `Instant` | `getCreationTimestamp()` → `Instant.ofEpochMilli()` |
| `endedAt` | `Instant` (nullable) | `getEndTimestamp()` → `null` si absent |

- **Fallback** : retourne `List.of()` — la synchronisation est sautée sans erreur.

---

## 📡 Endpoints Unblu utilisés

| Méthode | Endpoint | Utilisation |
|---------|----------|-------------|
| `POST` | `/v4/conversations/create` | Création conversation standard et directe |
| `POST` | `/v4/conversations/{id}/addParticipant` | Ajout du bot résumé |
| `POST` | `/v4/bots/sendMessage` | Envoi du message de résumé |
| `POST` | `/v4/conversations/search` | Listing de toutes les conversations |

---

## ⚠️ Gestion des Erreurs

| Code HTTP | Cause | Comportement |
|-----------|-------|-------------|
| `400` | Agent fourni n'est pas de type `AGENT` | `UnbluApiException(400)` levée avant l'appel API |
| `403` | Droits insuffisants sur la clé API | `UnbluApiException(403)` avec message explicite |
| `404` | Ressource inexistante (personne, conversation) | `UnbluApiException(404)` propagée |
| Autres | Erreur technique SDK | `UnbluApiException` avec code HTTP d'origine |
