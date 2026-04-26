# Kafka — Topics, messages et design pour le bot Unblu

---

## 1. C'est quoi un topic Kafka ?

Un **topic** est une file de messages persistée et ordonnée. C'est le canal de communication entre
deux services qui ne se connaissent pas directement.

```
Producteur  ──→  [  topic  ]  ──→  Consommateur
```

Analogie : un topic est comme une **boîte aux lettres partagée** — le producteur dépose un message,
le consommateur le ramasse quand il est prêt. Ni l'un ni l'autre n'a besoin que l'autre soit disponible
au même moment.

### Les trois concepts clés

| Concept | Ce que c'est | Dans ce projet |
|---|---|---|
| **Topic** | Le nom du canal | `unblu.webhook.events` |
| **Clé (key)** | Identifiant de routage du message | `eventType` (ex. `conversation.created`) |
| **Corps (value)** | Le contenu du message | JSON brut de l'événement Unblu |

La **clé** sert à garantir l'ordre : tous les messages avec la même clé vont sur la même partition,
donc ils sont traités dans l'ordre d'arrivée. Pour des événements de conversation, la clé est
typiquement le `conversationId` ou le `eventType`.

---

## 2. Ce qui existe aujourd'hui

### Topologie actuelle

```
Unblu
  │  webhook HTTP POST
  ▼
webhook-entrypoint (port 8083)
  │  publie JSON brut
  ▼
[unblu.webhook.events]          ← topic existant (3 partitions)
  │  consomme
  ▼
engine (port 8084, Camel)
  │  désérialise → WebhookEventRoute → ConversationEventProcessor
  ▼
PostgreSQL (historique conversations)
```

### Topics existants

| Topic | Partitions | Rôle | Producteur | Consommateur |
|---|---|---|---|---|
| `unblu.webhook.events` | 3 | Events entrants Unblu → traitement | `webhook-entrypoint` | `engine` |
| `unblu.webhook.events.dlq` | 1 | Messages non traitables (Dead Letter Queue) | `engine` | monitoring / reprise manuelle |

### Format du message dans `unblu.webhook.events`

**Clé :** `eventType` (ex. `conversation.created`, `conversation.new_message`)

**Corps (JSON brut tel qu'envoyé par Unblu) :**
```json
{
  "$_type": "ConversationCreatedEvent",
  "eventType": "conversation.created",
  "timestamp": 1714158475000,
  "accountId": "abc123",
  "conversationId": "WEuipEHdS1mU59j0aTvYkQ",
  "conversation": {
    "id": "WEuipEHdS1mU59j0aTvYkQ",
    "topic": "LiveKit Conversation",
    "state": "CREATED"
  }
}
```

Modèle Java : `UnbluWebhookPayload` dans `integration-domain`.

---

## 3. Ce qui manque — le pendant de sortie vers Unblu

Aujourd'hui le flux est **unidirectionnel** : Unblu → Kafka → engine.

Il n'existe pas de topic pour le sens inverse : une commande déposée par un service
qui doit déclencher un **appel REST vers Unblu**.

### Cas d'usage typiques (bot)

| Action | Déclencheur | Appel Unblu cible |
|---|---|---|
| Envoyer un message au visiteur | `dialog.opened` traité | `botsSendDialogMessage()` |
| Positionner le named area | contexte client résolu | `conversationsSetRecipient()` |
| Fermer le dialog (HAND_OFF) | traitement terminé | `botsFinishDialog(HAND_OFF)` |
| Positionner une URL | URL calculée | `conversationsSetCustomData()` ou message |

---

## 4. Topics à créer pour le bot

### Vue d'ensemble de la topologie cible

```
BotOutboundController (reçoit les events Unblu via ngrok)
  │  publie commande
  ▼
[unblu.bot.commands]             ← NOUVEAU
  │  consomme
  ▼
bot-engine (Camel, nouveau module ou extension engine)
  │  exécute l'orchestration (CRM, LLM, rules, URL)
  │  puis publie le résultat
  ▼
[unblu.bot.actions]              ← NOUVEAU
  │  consomme
  ▼
unblu-action-executor
  │  appel REST vers Unblu API
  ▼
Unblu (sendMessage, setRecipient, finishDialog...)
```

---

### Topic 1 — `unblu.bot.commands`

**Rôle :** transmettre les événements bot reçus d'Unblu vers le moteur de traitement.
C'est l'équivalent de `unblu.webhook.events` mais pour les outbound bot events.

**Clé :** `dialogToken` (garantit l'ordre des events d'un même dialog)

**Corps — exemple `dialog.opened` :**
```json
{
  "commandType": "BOT_DIALOG_OPENED",
  "correlationId": "f231180c",
  "dialogToken": "qsfFvt94R2O6xYOKNQ3Qrw-c-Qt8DWI...",
  "conversationId": "qsfFvt94R2O6xYOKNQ3Qrw",
  "timestamp": "2026-04-26T19:52:29.477Z",
  "rawPayload": "{ ... JSON Unblu original ... }"
}
```

**Corps — exemple `onboarding_offer` :**
```json
{
  "commandType": "BOT_ONBOARDING_OFFER",
  "correlationId": "7b0b1211",
  "dialogToken": null,
  "conversationId": null,
  "timestamp": "2026-04-26T19:52:19.055Z",
  "rawPayload": "{ ... }"
}
```

**Partitions :** 3 (parallélisme sur plusieurs conversations simultanées)

---

### Topic 2 — `unblu.bot.actions`

**Rôle :** transmettre les actions à exécuter vers Unblu après le traitement métier.
Découple l'orchestration (lent : CRM, LLM, règles) de l'exécution REST (rapide).

**Clé :** `dialogToken` (même clé que la commande d'origine → ordre garanti)

**Corps — exemple `SEND_MESSAGE` :**
```json
{
  "actionType": "SEND_MESSAGE",
  "correlationId": "f231180c",
  "dialogToken": "qsfFvt94R2O6xYOKNQ3Qrw-c-Qt8DWI...",
  "conversationId": "qsfFvt94R2O6xYOKNQ3Qrw",
  "payload": {
    "text": "Le client cherche des conseils...\n\nVotre espace : https://..."
  }
}
```

**Corps — exemple `SET_NAMED_AREA` :**
```json
{
  "actionType": "SET_NAMED_AREA",
  "correlationId": "f231180c",
  "dialogToken": null,
  "conversationId": "qsfFvt94R2O6xYOKNQ3Qrw",
  "payload": {
    "namedAreaId": "ZvcLavqFTKC65YtiRKtJxg"
  }
}
```

**Corps — exemple `HAND_OFF` :**
```json
{
  "actionType": "HAND_OFF",
  "correlationId": "f231180c",
  "dialogToken": "qsfFvt94R2O6xYOKNQ3Qrw-c-Qt8DWI...",
  "conversationId": "qsfFvt94R2O6xYOKNQ3Qrw",
  "payload": {
    "reason": "HAND_OFF"
  }
}
```

**Partitions :** 3

---

### Topic 3 — `unblu.bot.actions.dlq`

**Rôle :** messages d'action non exécutables (Unblu API en erreur après retry).
Même pattern que `unblu.webhook.events.dlq`.

**Partitions :** 1

---

## 5. Récapitulatif de tous les topics du projet

| Topic | Partitions | Producteur | Consommateur | Clé | Existe |
|---|---|---|---|---|---|
| `unblu.webhook.events` | 3 | `webhook-entrypoint` | `engine` | `eventType` | ✅ |
| `unblu.webhook.events.dlq` | 1 | `engine` | monitoring | — | ✅ |
| `unblu.bot.commands` | 3 | `BotOutboundController` | bot-engine | `dialogToken` | ❌ à créer |
| `unblu.bot.actions` | 3 | bot-engine | `unblu-action-executor` | `dialogToken` | ❌ à créer |
| `unblu.bot.actions.dlq` | 1 | `unblu-action-executor` | monitoring | — | ❌ à créer |

---

## 6. Comparaison architecture actuelle vs cible

| Critère | Actuel (`CompletableFuture`) | Cible (Kafka) |
|---|---|---|
| Découplage réception / traitement | Partiel (async mais in-process) | Total (services indépendants) |
| Retry sur erreur | Aucun | 3 tentatives + backoff |
| Persistance des événements | Non (perdu si crash JVM) | Oui (rétention Kafka configurable) |
| DLQ | Non | Oui (`unblu.bot.actions.dlq`) |
| Ordre garanti par dialog | Non | Oui (même clé = même partition) |
| Observabilité | Logs uniquement | Kafka UI + métriques consumer lag |
| Scalabilité | Limitée (ForkJoinPool) | Horizontale (ajouter des consumers) |

---

## 7. Évolution du `docker-compose.yml`

Ajouter dans `kafka-init` :

```yaml
kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --partitions 3 --replication-factor 1 \
  --topic unblu.bot.commands

kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --partitions 3 --replication-factor 1 \
  --topic unblu.bot.actions

kafka-topics --create --if-not-exists \
  --bootstrap-server kafka:29092 \
  --partitions 1 --replication-factor 1 \
  --topic unblu.bot.actions.dlq
```
