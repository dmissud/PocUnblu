# PocBotDialogService — Comportement et orchestration

## Rôle

`PocBotDialogService` est le cœur du bot Unblu dans le module `livekit`.
Il est déclenché par les événements outbound Unblu (via `BotOutboundController`) et orchestre,
de façon **asynchrone**, une séquence d'appels vers des systèmes externes avant de rendre la main
à un agent humain.

---

## Flux principal — `dialog.opened`

Lorsqu'Unblu notifie l'ouverture d'un dialog bot, `BotOutboundController` retourne un **ACK immédiat**
à Unblu (< 4ms) et délègue le traitement à `PocBotDialogService` sur un thread du `ForkJoinPool`.

```
Unblu → BotOutboundController → ACK immédiat
                              ↘ PocBotDialogService.onDialogOpened() [async]
```

### Séquence des étapes asynchrones

```mermaid
sequenceDiagram
    participant Bot as PocBotDialogService<br/>[ForkJoinPool]
    participant CRM as ClientContextPort<br/>(CRM/ERP)
    participant LLM as ConversationSummaryPort<br/>(LLM)
    participant Rules as NamedAreaResolverPort<br/>(Moteur de règles)
    participant Docs as ResourceUrlPort<br/>(Service documentaire)
    participant Unblu as Unblu API

    activate Bot
    Note over Bot: ASYNC_START

    Bot->>CRM: resolveClientContext(conversationId)
    CRM-->>Bot: ClientContext(clientId, segment, language)
    Note over Bot: CLIENT_CONTEXT — ~80-180ms

    Bot->>LLM: generateSummary(conversationId)
    LLM-->>Bot: résumé texte
    Note over Bot: SUMMARY_GENERATED — ~2ms (mock)

    Bot->>Rules: resolveNamedAreaId(clientContext)
    Rules-->>Bot: namedAreaId selon segment
    Note over Bot: NAMED_AREA_RESOLVED — ~50-120ms

    Bot->>Unblu: conversationsSetRecipient(namedAreaId)
    Unblu-->>Bot: OK / ERROR
    Note over Bot: SET_NAMED_AREA — ~40-80ms

    Bot->>Docs: computeResourceUrl(clientContext)
    Docs-->>Bot: URL contextuelle
    Note over Bot: URL_COMPUTED — ~60-160ms

    Bot->>Unblu: botsSendDialogMessage(résumé + URL)
    Unblu-->>Bot: OK / ERROR
    Note over Bot: SEND_MESSAGE — ~180-220ms

    Bot->>Unblu: botsFinishDialog(HAND_OFF)
    Unblu-->>Bot: OK / ERROR
    Note over Bot: HAND_OFF — ~110-140ms

    deactivate Bot
    Note over Bot: ASYNC_DONE — total estimé ~530-920ms
```

---

## Description des étapes

### Étape 1 — Résolution du contexte client (`ClientContextPort`)

**But :** identifier qui est le client pour les étapes suivantes.  
**Entrée :** `conversationId`  
**Sortie :** `ClientContext(clientId, segment, language)`  
**Mock :** `ClientContextMockAdapter` — dérive un `clientId` depuis le `conversationId`,
tire aléatoirement un segment (`VIP`, `PREMIUM`, `STANDARD`) et une langue (`fr`, `en`, `de`).  
**Latence simulée :** 80–180ms  
**Système réel cible :** CRM / ERP

---

### Étape 2 — Génération du résumé (`ConversationSummaryPort`)

**But :** produire un résumé de la demande client à destination du conseiller.  
**Entrée :** `conversationId`  
**Sortie :** texte libre (2 phrases)  
**Mock :** `ConversationSummaryMockAdapter` — combine aléatoirement deux phrases prédéfinies.
Protégé par un `@CircuitBreaker(name = "summary")`.  
**Latence simulée :** < 5ms  
**Système réel cible :** LLM (ex. Claude API)

---

### Étape 3 — Résolution du named area (`NamedAreaResolverPort`)

**But :** déterminer la file d'attente / l'équipe Unblu cible en fonction du segment client.  
**Entrée :** `ClientContext`  
**Sortie :** `namedAreaId` (identifiant Unblu)  
**Mock :** `NamedAreaResolverMockAdapter` — applique une table de correspondance :

| Segment | Named area |
|---|---|
| VIP | `named-area-vip-desk` |
| PREMIUM | `named-area-premium-desk` |
| STANDARD | `named-area-standard-desk` |

**Latence simulée :** 50–120ms  
**Système réel cible :** moteur de règles métier

#### Positionnement dans Unblu

Immédiatement après la résolution, le service appelle `conversationsSetRecipient()` sur l'API Unblu
pour router la conversation vers le bon desk.

```
Unblu API : conversationsSetRecipient(conversationId, namedAreaId)
```

---

### Étape 4 — Calcul de l'URL contextuelle (`ResourceUrlPort`)

**But :** générer un lien vers la fiche client ou le document pertinent, à envoyer au visiteur.  
**Entrée :** `ClientContext`  
**Sortie :** URL sous la forme `https://client-portal.example.com/clients/{clientId}/conversations/{conversationId}?segment=...&lang=...`  
**Mock :** `ResourceUrlMockAdapter` — construit l'URL à partir des données du contexte client.  
**Latence simulée :** 60–160ms  
**Système réel cible :** service de gestion documentaire / portail client

#### Envoi au visiteur

L'URL est concaténée au résumé LLM dans un seul message envoyé via `botsSendDialogMessage()` :

```
<résumé>

Votre espace personnel : <url>
```

---

### Étape 5 — Transfert vers les agents (`HAND_OFF`)

**But :** fermer le dialog bot et remettre la conversation dans la file des agents.  
**Appel :** `botsFinishDialog(reason = HAND_OFF)`  
**Effet :** Unblu déclenche immédiatement un événement `dialog.closed` (+3–10ms).

---

## Autres événements traités

| Événement Unblu | Comportement |
|---|---|
| `onboarding_offer` | Accepté systématiquement (`offerAccepted=true`) |
| `reboarding_offer` | Accepté systématiquement |
| `offboarding_offer` | Accepté systématiquement |
| `dialog.message` | Ignoré si `senderType ≠ VISITOR` (filtre les échos bot) |
| `dialog.closed` | Log uniquement |

---

## Traçage — tags de log

Tous les logs de ce service utilisent des tags structurés `key=value` pour faciliter l'analyse.

| Tag | Émetteur | Signification |
|---|---|---|
| `[BOT_TRACE]` | `PocBotDialogService` | Étape du flux async avec `correlationId` et `durationMs` |
| `[BOT_EVENT]` | `BotOutboundController` | Réception et dispatch d'un event Unblu |
| `[CONV_TRACE]` | `LiveKitConversationService` | Étapes de la création de conversation |
| `[EXT_TRACE]` | Mocks adaptateurs | Résultat retourné par le système externe simulé |

### Exemple de séquence de logs nominale

```
[BOT_EVENT]  correlationId=f9513be7 event=dialog.opened dialogToken=... conversationId=...
[BOT_TRACE]  step=ASYNC_START       correlationId=f9513be7 ...
[EXT_TRACE]  step=CRM_RESOLVED      conversationId=... clientId=... segment=VIP language=fr
[BOT_TRACE]  step=CLIENT_CONTEXT    correlationId=f9513be7 segment=VIP language=fr durationMs=143
[EXT_TRACE]  step=LLM_SUMMARY       conversationId=...
[BOT_TRACE]  step=SUMMARY_GENERATED correlationId=f9513be7 summaryLength=131 durationMs=2
[EXT_TRACE]  step=NAMED_AREA_RESOLVED conversationId=... namedAreaId=named-area-vip-desk
[BOT_TRACE]  step=NAMED_AREA_RESOLVED correlationId=f9513be7 namedAreaId=named-area-vip-desk durationMs=87
[BOT_TRACE]  step=SET_NAMED_AREA    correlationId=f9513be7 status=OK durationMs=52
[EXT_TRACE]  step=URL_COMPUTED      conversationId=... url=https://client-portal.example.com/...
[BOT_TRACE]  step=URL_COMPUTED      correlationId=f9513be7 url=... durationMs=112
[BOT_TRACE]  step=SEND_MESSAGE      correlationId=f9513be7 status=OK durationMs=196
[BOT_TRACE]  step=HAND_OFF          correlationId=f9513be7 status=OK durationMs=128
[BOT_TRACE]  step=ASYNC_DONE        correlationId=f9513be7 totalDurationMs=720
```

---

## Budget temps estimé (mock)

| Étape | Min | Max | Typique |
|---|---|---|---|
| CRM (`ClientContextPort`) | 80ms | 180ms | ~130ms |
| LLM (`ConversationSummaryPort`) | 0ms | 5ms | ~2ms |
| Moteur de règles (`NamedAreaResolverPort`) | 50ms | 120ms | ~85ms |
| `SET_NAMED_AREA` (Unblu API) | 40ms | 80ms | ~55ms |
| Service documentaire (`ResourceUrlPort`) | 60ms | 160ms | ~110ms |
| `SEND_MESSAGE` (Unblu API) | 180ms | 220ms | ~195ms |
| `HAND_OFF` (Unblu API) | 110ms | 140ms | ~125ms |
| **Total async** | **520ms** | **905ms** | **~700ms** |

En production, le poste dominant sera la génération LLM (cible : < 2s avec streaming).

---

## Remplacement des mocks par les implémentations réelles

Chaque mock implémente une interface port — le remplacement se fait sans modifier le service :

| Port | Mock actuel | Implémentation cible |
|---|---|---|
| `ClientContextPort` | `ClientContextMockAdapter` | Adapter CRM/ERP |
| `ConversationSummaryPort` | `ConversationSummaryMockAdapter` | Adapter Claude API (LLM) |
| `NamedAreaResolverPort` | `NamedAreaResolverMockAdapter` | Adapter moteur de règles |
| `ResourceUrlPort` | `ResourceUrlMockAdapter` | Adapter portail client |
