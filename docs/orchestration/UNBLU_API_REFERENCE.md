# Référence API Unblu - PoC

## 📋 Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [PersonsApi](#personsapi)
3. [ConversationsApi](#conversationsapi)
4. [TeamsApi](#teamsapi)
5. [BotsApi](#botsapi)

---

## 🎯 Vue d'ensemble

Ce document liste tous les endpoints Unblu utilisés dans le PoC, avec des exemples de requêtes et réponses au format REST.

### Configuration

**Base URL** : `https://your-unblu-server/rest/v4`

**Authentification** : API Key dans le header
```
X-Unblu-Apikey: your-api-key-here
```

### APIs utilisées

| API | Endpoints utilisés | Contexte d'utilisation |
|-----|-------------------|------------------------|
| PersonsApi | 5 endpoints | Gestion des personnes (clients et agents) |
| ConversationsApi | 2 endpoints | Création et gestion des conversations |
| TeamsApi | 1 endpoint | Recherche d'équipes |
| BotsApi | 2 endpoints | Création de bots et envoi de messages |

---

## 📱 PersonsApi

### 1. GET /persons/getBySource - Récupérer une personne par source externe

**Usage** : Obtenir l'ID Unblu interne d'une personne à partir de son sourceId externe.

**Contexte** : Workflow création de conversation avec équipe (pour résoudre le personId du client).

**Service Java** : `UnbluService.getPersonBySource()` - ligne 228

**Endpoint REST** :
```http
GET /rest/v4/persons/getBySource?personSource={personSource}&sourceId={sourceId}
```

**Paramètres** :
- `personSource` : `VIRTUAL` ou `USER_DB`
- `sourceId` : Identifiant externe de la personne

**Exemple de requête** :
```http
GET /rest/v4/persons/getBySource?personSource=VIRTUAL&sourceId=client-12345
X-Unblu-Apikey: your-api-key-here
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "PersonData",
  "id": "V9oXS87OQ2ad9Qw7z0hDZA",
  "creationTimestamp": 1710777600000,
  "modificationTimestamp": 1710777600000,
  "version": 1,
  "accountId": "account-id",
  "personSource": "VIRTUAL",
  "sourceId": "client-12345",
  "sourceData": null,
  "firstName": "Jean",
  "lastName": "Dupont",
  "username": null,
  "nickname": null,
  "displayName": "Jean Dupont",
  "personLabels": [],
  "avatar": null,
  "email": "jean.dupont@example.com",
  "phone": "+33612345678",
  "teamId": null,
  "teamName": null,
  "authorization": {
    "$_type": "VirtualPersonAuthorization"
  }
}
```

**Mapping vers objet métier** :
```java
PersonData → PersonInfo(
    id = "V9oXS87OQ2ad9Qw7z0hDZA",
    sourceId = "client-12345",
    displayName = "Jean Dupont",
    email = "jean.dupont@example.com"
)
```

---

### 2. POST /persons/search - Rechercher des personnes avec filtres

**Usage** : Rechercher des personnes selon leur source (VIRTUAL, USER_DB) et/ou sourceId.

**Contexte** : Workflow conversation directe 1-to-1 (recherche client VIRTUAL et agent USER_DB).

**Service Java** : `UnbluService.searchPersons()` - ligne 108

**Endpoint REST** :
```http
POST /rest/v4/persons/search
```

**Exemple de requête** (recherche client VIRTUAL) :
```http
POST /rest/v4/persons/search
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "PersonQuery",
  "searchFilters": [
    {
      "$_type": "PersonSourcePersonSearchFilter",
      "field": "PERSON_SOURCE",
      "operator": "EQUALS",
      "value": "VIRTUAL"
    },
    {
      "$_type": "SourceIdPersonSearchFilter",
      "field": "SOURCE_ID",
      "operator": "EQUALS",
      "value": "client-12345"
    }
  ],
  "orderBy": [],
  "offset": 0,
  "limit": 10
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "PersonResult",
  "items": [
    {
      "$_type": "PersonData",
      "id": "V9oXS87OQ2ad9Qw7z0hDZA",
      "personSource": "VIRTUAL",
      "sourceId": "client-12345",
      "displayName": "Jean Dupont",
      "email": "jean.dupont@example.com",
      "firstName": "Jean",
      "lastName": "Dupont"
    }
  ],
  "hasMoreItems": false,
  "totalCount": 1,
  "offset": 0,
  "limit": 10
}
```

**Exemple de requête** (recherche agent USER_DB disponible) :
```http
POST /rest/v4/persons/search
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "PersonQuery",
  "searchFilters": [
    {
      "$_type": "PersonSourcePersonSearchFilter",
      "field": "PERSON_SOURCE",
      "operator": "EQUALS",
      "value": "USER_DB"
    }
  ],
  "orderBy": [],
  "offset": 0,
  "limit": 1
}
```

---

### 3. POST /persons/searchAgents - Rechercher des agents disponibles

**Usage** : Rechercher des agents avec filtres avancés (statut, équipe, etc.).

**Contexte** : (Non utilisé actuellement dans le PoC, préparé pour routage avancé).

**Service Java** : `UnbluService.searchAgents()` - ligne 185

**Endpoint REST** :
```http
POST /rest/v4/persons/searchAgents
```

**Exemple de requête** :
```http
POST /rest/v4/persons/searchAgents
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "AgentPersonQuery",
  "searchFilters": [
    {
      "$_type": "PersonSourcePersonSearchFilter",
      "field": "PERSON_SOURCE",
      "operator": "EQUALS",
      "value": "USER_DB"
    }
  ],
  "orderBy": [],
  "offset": 0,
  "limit": 10
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "AgentPersonResult",
  "items": [
    {
      "$_type": "AgentPersonData",
      "id": "agent-001",
      "personSource": "USER_DB",
      "sourceId": "agent.smith@company.com",
      "displayName": "Agent Smith",
      "email": "agent.smith@company.com",
      "teamId": "sales-team-01",
      "teamName": "Équipe Ventes",
      "statusMessage": {
        "$_type": "AgentAvailabilityState",
        "type": "AVAILABLE",
        "pauseNotificationsMode": "OFF",
        "statusMessage": null
      }
    }
  ],
  "hasMoreItems": false,
  "totalCount": 1
}
```

---

### 4. POST /persons/searchAgentsByState - Rechercher agents par état

**Usage** : Rechercher des agents selon leur disponibilité (AVAILABLE, BUSY, AWAY, etc.).

**Contexte** : (Non utilisé actuellement, préparé pour routage intelligent).

**Service Java** : `UnbluService.searchAgentsByState()` - ligne 209

**Endpoint REST** :
```http
POST /rest/v4/persons/searchAgentsByState
```

**Exemple de requête** :
```http
POST /rest/v4/persons/searchAgentsByState
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "AgentStateSearchQuery",
  "teamId": "sales-team-01",
  "availabilityStates": ["AVAILABLE", "BUSY"],
  "orderBy": [],
  "offset": 0,
  "limit": 10
}
```

---

### 5. POST /persons/createOrUpdateBot - Créer ou mettre à jour un bot

**Usage** : Créer une personne de type bot pour envoyer des messages.

**Contexte** : Création du bot "Summary Bot" pour envoyer des résumés de conversation.

**Service Java** : `UnbluService.createBotPerson()` - ligne 267

**Endpoint REST** :
```http
POST /rest/v4/persons/createOrUpdateBot
```

**Exemple de requête** :
```http
POST /rest/v4/persons/createOrUpdateBot
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "PersonData",
  "personSource": "BOT",
  "sourceId": "bot-summary-bot",
  "displayName": "Summary Bot",
  "firstName": "Summary",
  "lastName": "Bot",
  "email": "bot-summary@company.com",
  "avatar": null
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "PersonData",
  "id": "bot-person-id-001",
  "personSource": "BOT",
  "sourceId": "bot-summary-bot",
  "displayName": "Summary Bot",
  "email": "bot-summary@company.com",
  "creationTimestamp": 1710777600000,
  "modificationTimestamp": 1710777600000,
  "version": 1
}
```

---

## 💬 ConversationsApi

### 1. POST /conversations/create - Créer une conversation

**Usage** : Créer une conversation avec une équipe ou en mode 1-to-1.

**Contexte** :
- Workflow conversation avec équipe : création avec recipient = TEAM
- Workflow conversation directe : création avec CONTEXT_PERSON + ASSIGNED_AGENT

**Service Java** : `UnbluService.createConversation()` - ligne 89

**Endpoint REST** :
```http
POST /rest/v4/conversations/create
```

**Exemple de requête** (Conversation avec équipe) :
```http
POST /rest/v4/conversations/create
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "ConversationCreationData",
  "accountId": "your-account-id",
  "topic": "Contact depuis Application Web",
  "visitorData": "client-12345",
  "initialEngagementType": "CHAT_REQUEST",
  "locale": "fr",
  "tokboxSessionId": null,
  "scheduledTimestamp": null,
  "externalMessengerChannelId": null,
  "sourceId": null,
  "sourceUrl": null,
  "conversationTemplateId": null,
  "recipient": {
    "$_type": "ConversationCreationRecipientData",
    "type": "TEAM",
    "id": "sales-team-01"
  },
  "participants": [
    {
      "$_type": "ConversationCreationParticipantData",
      "participationType": "CONTEXT_PERSON",
      "personId": "V9oXS87OQ2ad9Qw7z0hDZA",
      "hidden": false,
      "conversationStarred": false
    }
  ],
  "initialEngagementUrl": null,
  "conversationVisibility": "PRIVATE",
  "metadata": {}
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "ConversationData",
  "id": "conv-abc123def456",
  "creationTimestamp": 1710777600000,
  "endTimestamp": null,
  "accountId": "your-account-id",
  "topic": "Contact depuis Application Web",
  "recipient": {
    "$_type": "TeamConversationRecipientData",
    "type": "TEAM",
    "id": "sales-team-01",
    "name": "Équipe Ventes"
  },
  "participants": [
    {
      "$_type": "ConversationParticipationData",
      "state": "ACTIVE",
      "personId": "V9oXS87OQ2ad9Qw7z0hDZA",
      "connectedViaExternalMessenger": false
    }
  ],
  "state": "ONBOARDING",
  "initialEngagementType": "CHAT_REQUEST",
  "locale": "fr",
  "tokboxSessionId": null,
  "visitorData": "client-12345",
  "conversationTemplateId": null,
  "links": [],
  "externalMessengerChannelId": null,
  "sourceId": null,
  "sourceUrl": null,
  "scheduledTimestamp": null,
  "metadata": {},
  "configuration": {
    "$_type": "ConversationConfiguration"
  }
}
```

**Exemple de requête** (Conversation directe 1-to-1) :
```http
POST /rest/v4/conversations/create
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "ConversationCreationData",
  "accountId": "your-account-id",
  "topic": "Demande d'assistance technique",
  "visitorData": "client-12345",
  "initialEngagementType": "CHAT_REQUEST",
  "locale": "fr",
  "recipient": null,
  "participants": [
    {
      "$_type": "ConversationCreationParticipantData",
      "participationType": "CONTEXT_PERSON",
      "personId": "V9oXS87OQ2ad9Qw7z0hDZA",
      "hidden": false
    },
    {
      "$_type": "ConversationCreationParticipantData",
      "participationType": "ASSIGNED_AGENT",
      "personId": "agent-001",
      "hidden": false
    }
  ],
  "conversationVisibility": "PRIVATE",
  "metadata": {}
}
```

**Mapping vers objet métier** :
```java
ConversationData → ConversationContext(
    unbluConversationId = "conv-abc123def456",
    unbluJoinUrl = null (généré par Unblu dans l'UI)
)
```

---

### 2. POST /conversations/{conversationId}/addParticipant - Ajouter un participant

**Usage** : Ajouter un participant (ex: bot) à une conversation existante.

**Contexte** : Ajout du bot Summary Bot après création de la conversation.

**Service Java** : `UnbluService.addBotToConversation()` - ligne 318

**Endpoint REST** :
```http
POST /rest/v4/conversations/{conversationId}/addParticipant
```

**Exemple de requête** :
```http
POST /rest/v4/conversations/conv-abc123def456/addParticipant
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "ConversationsAddParticipantBody",
  "personId": "bot-person-id-001",
  "escalationLevel": null,
  "hidden": true,
  "conversationStarred": false
}
```

**Paramètres** :
- `personId` : ID de la personne à ajouter (bot)
- `hidden` : `true` pour masquer le participant aux autres (recommandé pour les bots)

**Exemple de réponse** (204 No Content) :
```
(Pas de body)
```

---

## 👥 TeamsApi

### 1. POST /teams/search - Rechercher des équipes

**Usage** : Lister toutes les équipes Unblu disponibles.

**Contexte** : Endpoint `GET /api/teams` exposé pour le frontend.

**Service Java** : `UnbluService.searchTeams()` - ligne 156

**Endpoint REST** :
```http
POST /rest/v4/teams/search
```

**Exemple de requête** :
```http
POST /rest/v4/teams/search
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "TeamQuery",
  "searchFilters": [],
  "orderBy": [
    {
      "$_type": "TeamOrderBy",
      "field": "NAME",
      "order": "ASCENDING"
    }
  ],
  "offset": 0,
  "limit": 100
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "TeamResult",
  "items": [
    {
      "$_type": "TeamData",
      "id": "sales-team-01",
      "creationTimestamp": 1710777600000,
      "modificationTimestamp": 1710777600000,
      "version": 1,
      "accountId": "your-account-id",
      "avatar": null,
      "name": "Équipe Ventes",
      "description": "Équipe commerciale pour les demandes de vente",
      "parentId": null,
      "siteId": null,
      "metadata": {}
    },
    {
      "$_type": "TeamData",
      "id": "support-team-01",
      "name": "Équipe Support",
      "description": "Support technique niveau 1",
      "metadata": {}
    }
  ],
  "hasMoreItems": false,
  "totalCount": 2,
  "offset": 0,
  "limit": 100
}
```

**Mapping vers objet métier** :
```java
TeamData → TeamInfo(
    id = "sales-team-01",
    name = "Équipe Ventes",
    description = "Équipe commerciale pour les demandes de vente"
)
```

---

## 🤖 BotsApi

### 1. POST /bots/create - Créer un bot custom

**Usage** : Créer un bot custom pour envoyer des messages dans les conversations.

**Contexte** : Création du bot Summary Bot au démarrage de l'application.

**Service Java** : `UnbluService.createBot()` - ligne 272

**Endpoint REST** :
```http
POST /rest/v4/bots/create
```

**Exemple de requête** :
```http
POST /rest/v4/bots/create
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "CustomDialogBotData",
  "accountId": "your-account-id",
  "name": "Summary Bot",
  "description": "Bot automatique pour générer des résumés de conversation",
  "type": "CUSTOM",
  "botPersonId": "bot-person-id-001",
  "onboardingFilter": "NONE",
  "offboardingFilter": "NONE",
  "reboardingEnabled": false,
  "webhookStatus": "INACTIVE",
  "webhookEndpoint": "https://your-server.com/webhook/bot-summary",
  "webhookApiVersion": "V4",
  "outboundTimeoutMillis": 5000,
  "metadata": {}
}
```

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "CustomDialogBotData",
  "id": "bot-summary-001",
  "creationTimestamp": 1710777600000,
  "modificationTimestamp": 1710777600000,
  "version": 1,
  "accountId": "your-account-id",
  "name": "Summary Bot",
  "description": "Bot automatique pour générer des résumés de conversation",
  "type": "CUSTOM",
  "botPersonId": "bot-person-id-001",
  "onboardingFilter": "NONE",
  "offboardingFilter": "NONE",
  "webhookStatus": "INACTIVE",
  "webhookEndpoint": "https://your-server.com/webhook/bot-summary",
  "webhookApiVersion": "V4",
  "outboundTimeoutMillis": 5000
}
```

---

### 2. POST /bots/sendMessage - Envoyer un message bot

**Usage** : Envoyer un message texte dans une conversation via un bot.

**Contexte** : Envoi du résumé de conversation généré par le mock adapter.

**Service Java** : `UnbluService.sendBotMessage()` - ligne 293

**Endpoint REST** :
```http
POST /rest/v4/bots/sendMessage
```

**Exemple de requête** :
```http
POST /rest/v4/bots/sendMessage
X-Unblu-Apikey: your-api-key-here
Content-Type: application/json

{
  "$_type": "BotPostMessage",
  "accountId": "your-account-id",
  "conversationId": "conv-abc123def456",
  "senderPersonId": "bot-person-id-001",
  "messageData": {
    "$_type": "PostMessageData",
    "type": "TEXT",
    "text": "📋 **Résumé de la conversation**\n\nLe client a contacté le service pour une demande d'information sur ses produits.\nUn conseiller spécialisé a été assigné pour traiter la demande en priorité.",
    "fallbackText": "Résumé de la conversation : Le client a contacté le service..."
  }
}
```

**Paramètres** :
- `conversationId` : ID de la conversation cible
- `senderPersonId` : ID du bot (personId créé avec createOrUpdateBot)
- `messageData.type` : `TEXT`, `IMAGE`, `FILE`, etc.
- `messageData.text` : Contenu du message (supporte Markdown)
- `messageData.fallbackText` : Texte de secours si Markdown non supporté

**Exemple de réponse** (200 OK) :
```json
{
  "$_type": "BotPostMessageResponse",
  "messageId": "msg-xyz789",
  "timestamp": 1710777660000
}
```

---

## 📊 Récapitulatif des endpoints par workflow

### Workflow 1 : Création conversation avec équipe

```
1. GET  /persons/getBySource?personSource=VIRTUAL&sourceId={clientId}
   → Récupération du personId interne

2. POST /conversations/create
   → Création conversation avec recipient.type=TEAM

3. POST /conversations/{conversationId}/addParticipant
   → Ajout du bot Summary Bot

4. POST /bots/sendMessage
   → Envoi du résumé de conversation
```

### Workflow 2 : Création conversation directe 1-to-1

```
1. POST /persons/search (filter: VIRTUAL)
   → Recherche du client

2. POST /persons/search (filter: USER_DB)
   → Recherche d'un agent disponible

3. POST /conversations/create
   → Création conversation avec participants: CONTEXT_PERSON + ASSIGNED_AGENT

4. POST /bots/sendMessage
   → Envoi du résumé de conversation
```

### Workflow 3 : Initialisation du bot (au démarrage)

```
1. POST /persons/createOrUpdateBot
   → Création de la personne bot

2. POST /bots/create
   → Création du bot custom
```

---

## 🔗 Documentation officielle Unblu

**API Reference** : https://www.unblu.com/en/doc/latest/unblu-web-api-reference

**Version utilisée** : v4 (8.30.1)

---

**Auteur** : Documentation générée à partir de l'analyse du code
**Date** : 2026-03-18
**Version** : 1.0
