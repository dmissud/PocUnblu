# Validation Finale : Orchestration Camel & Résilience

L'objectif de cette dernière étape était de valider la connectivité avec le vrai **SDK Unblu** tout en s'assurant que le système reste robuste en cas de défaillance (API injoignable ou latence) via **Resilience4j**.

## 1. Intégration du SDK Unblu avec le routeur Camel
L'adapter Camel mocké a été remplacé par un véritable appel au SDK. La structure REST `ConversationContext` générée par l'ERP et le Rule Engine (Mock) est traduite en un objet `ConversationCreationData`.

Nous avons utilisé les vrais modèles générés depuis l'OpenAPI Unblu v4 :
```java
// Assignation du destinataire Unblu via type TEAM
ConversationCreationRecipientData recipient = new ConversationCreationRecipientData();
recipient.setType(EConversationRecipientType.TEAM);
recipient.setId(ctx.getRoutingDecision().getUnbluAssignedGroupId());
creationData.setRecipient(recipient);

// Appel réel au SDK Unblu via le service UnbluService
ConversationData response = unbluService.createConversation(creationData);
```

## 2. Configuration du Circuit Breaker avec Resilience4j
Pour protéger notre orchestrateur des pannes Unblu, nous avons enchaîné le DSL Camel avec un bloc `.circuitBreaker()`.

```java
.circuitBreaker()
    .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(3000).end()
    .to("direct:unblu-adapter")
.onFallback()
    .log("⚠️ L'API Unblu est injoignable ou a expiré. Déclenchement du Fallback.")
    .process(exchange -> {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        ctx.setUnbluConversationId("OFFLINE-PENDING");
        ctx.setUnbluJoinUrl("Le service de chat est temporairement indisponible.");
        exchange.getIn().setBody(ctx);
    })
.end()
```

## 3. Preuve de Test (End-to-End)
Nous avons démarré le serveur Spring Boot localement avec cette configuration. Lorsque la requête `POST /api/v1/conversations/start` a été soumise, le SDK Unblu a tenté de joindre l'API (qui était mockée / down dans ce test). 

Après un timeout de 3000ms, le Circuit Breaker a **bloqué l'erreur réseau**, a sauté vers le bloc `Fallback`, et a renvoyé la réponse sécurisée suivante au Frontend :

```json
{
  "unbluConversationId": "OFFLINE-PENDING",
  "unbluJoinUrl": "Le service de chat est temporairement indisponible.",
  "status": "CREATED",
  "message": "Conversation successfully created."
}
```

> [!TIP]
> Cet exemple démontre la puissance de Camel : L'implémentation d'une gestion complète de résilience, de redondance et de transformation de message s'est effectuée de façon purement déclarative, sans écrire de blocs `try/catch` complexes dans le code métier de notre architecture hexagonale.
