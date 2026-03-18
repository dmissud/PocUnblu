# Service Webhook Unblu

## 📋 Vue d'ensemble

Service opérationnel pour recevoir et traiter les événements webhook d'Unblu, implémenté selon l'architecture hexagonale (ports & adapters) avec orchestration Apache Camel.

---

## 🎯 Types d'événements Unblu disponibles (SDK v4 - 8.30.1)

### Événements Conversation
- `ConversationCreatedEvent` - Conversation créée
- `ConversationEndedEvent` - Conversation terminée
- `ConversationActiveEvent` - Conversation activée
- `ConversationOnboardingEvent` - Début d'onboarding
- `ConversationRequeuedEvent` - Conversation remise en file d'attente
- `ConversationRecordingStartedEvent` - Enregistrement démarré

### Événements Message
- `ConversationMessageStateEvent` - État du message modifié
- `ConversationMessageTranslatedEvent` - Message traduit
- `BotDialogMessageEvent` - Message de bot
- `ExternalMessengerNewMessageEvent` - Nouveau message externe
- `ExternalMessengerMessageStateEvent` - État message externe

### Événements Participant
- `ParticipationActivatedEvent` - Participant activé
- `ParticipationLeftEvent` - Participant quitté
- `CallParticipantJoinedEvent` - Participant rejoint appel

### Événements Résumé
- `ConversationSummaryGeneratedEvent` - Résumé généré
- `ConversationSummaryFailedEvent` - Échec génération résumé
- `ConversationSummaryRejectedEvent` - Résumé rejeté

### Événements Invitation/Agent
- `AgentInvitationCreatedEvent` - Invitation agent créée
- `AgentInvitationRevokedEvent` - Invitation agent révoquée
- `AgentForwardingCreatedEvent` - Transfert agent créé

### Événements Système
- `WebhookAuditEvent` - Audit webhook
- `WebhookExpiredEvent` - Webhook expiré
- `WebhookPingEvent` - Ping webhook (test)

---

## 🏗️ Architecture Implémentée

### Domain Layer (`unblu-domain`)
- `WebhookSetupResult` : Résultat du setup webhook
- `WebhookStatus` : État du webhook et ngrok

### Application Layer (`unblu-application`)
- `SetupWebhookUseCase` : Interface pour setup/status/teardown
- `WebhookSetupService` : Orchestration du setup webhook avec ngrok
- `WebhookEventRoute` : Route Camel pour traiter les événements (`direct:webhook-event-processor`)
  - Routing par type d'événement (conversation, person, unknown)
  - Handlers dédiés pour chaque type

### Infrastructure Layer (`unblu-infrastructure`)
- `NgrokManager` : Gestion du tunnel ngrok
- `UnbluService` : Méthodes webhook (createWebhook, updateWebhook, deleteWebhook, getWebhookByName)

### Exposition Layer (`unblu-exposition`)
- `RestExpositionRoute` : Endpoints REST
  - `POST /api/v1/webhooks/setup` : Configuration du webhook
  - `GET /api/v1/webhooks/status` : État du webhook
  - `DELETE /api/v1/webhooks/teardown` : Suppression du webhook
  - `POST /api/webhooks/unblu` : Réception des événements Unblu
- `WebhookReceiverRoute` : Traitement interne des webhooks reçus (`direct:webhook-receiver-internal`)

---

## 📦 Configuration

### application.properties
```properties
unblu.webhook.name=unblu-poc-webhook
unblu.webhook.events=CONVERSATION.CREATED
unblu.webhook.endpoint-path=/api/webhooks/unblu
```

### Endpoints REST

#### Setup Webhook
```bash
POST /api/v1/webhooks/setup
```
Démarre ngrok, crée/met à jour le webhook dans Unblu.

#### Status Webhook
```bash
GET /api/v1/webhooks/status
```
Retourne l'état de ngrok et du webhook.

#### Teardown Webhook
```bash
DELETE /api/v1/webhooks/teardown?deleteWebhook=false
```
Arrête ngrok, supprime optionnellement le webhook d'Unblu.

#### Réception Événements
```bash
POST /api/webhooks/unblu
```
Endpoint appelé par Unblu pour envoyer les événements.

---

## 🔄 Workflow Simplifié

```
Unblu → POST /api/webhooks/unblu → WebhookReceiverRoute
                                    ↓
                            Parse JSON payload
                                    ↓
                       direct:webhook-event-processor
                                    ↓
                        WebhookEventRoute (routing)
                                    ↓
                ┌───────────────────┼───────────────────┐
                ↓                   ↓                   ↓
    conversation handler    person handler    unknown handler
                ↓                   ↓                   ↓
           Log events          Log events         Log events
```

## 📝 Endpoints Camel

| Endpoint                              | Description                       |
|---------------------------------------|-----------------------------------|
| `direct:webhook-receiver-internal`    | Point d'entrée REST               |
| `direct:webhook-event-processor`      | Traitement principal              |
| `direct:webhook-handle-conversation`  | Handler événements conversation   |
| `direct:webhook-handle-person`        | Handler événements person         |
| `direct:webhook-handle-unknown`       | Handler événements inconnus       |

---

## 🌐 Ngrok (Implémenté)

Le service utilise `NgrokManager` pour gérer automatiquement le tunnel ngrok :
- Démarrage automatique via `POST /api/v1/webhooks/setup`
- Récupération de l'URL publique
- Arrêt via `DELETE /api/v1/webhooks/teardown`

**Note** : Avec ngrok gratuit, l'URL change à chaque redémarrage.
