# Construction de la Réponse (API REST)

La réponse finale renvoyée au système initiateur (celui qui a fait le `POST /api/v1/conversations/start`) est construite en plusieurs étapes via l'Orchestrateur Camel. L'objectif est de toujours masquer la complexité interne (ERP, Moteur de Règles, Unblu) et d'offrir un contrat d'interface HTTP simple et clair.

Voici comment la réponse est construite pas-à-pas :

## 1. Le Contrat d'Interface (DTO)
Le format de la réponse est défini par la classe `StartConversationResponse` :
```java
public class StartConversationResponse {
    private String unbluConversationId;
    private String unbluJoinUrl;
    private String status;
    private String message;
}
```
Ce format permet au système appelant de savoir immédiatement si la demande a fonctionné (`status` et `message`), et si oui, comment y accéder (`unbluConversationId` et `unbluJoinUrl`).

## 2. Le Mapping de la réponse de Succès (Nominal / Fallback)
Tout au long de la route Camel, les informations s'accumulent dans un objet central appelé le `ConversationContext`. 
À la toute fin de la route principale (étape 6 dans `MainOrchestratorRoute`), le contexte complet est passé à notre `DomainMapper` pour générer le DTO public :

```java
// Dans MainOrchestratorRoute.java :
// 6. Transformer le 'ConversationContext' final en 'StartConversationResponse' pour le client
.bean(DomainMapper.class, "toFrontendResponse");
```

Le mapper prend les données générées par l'Adapter Unblu (ou le fallback Resilience4j) et met en forme le succès :
```java
// Dans DomainMapper.java :
public StartConversationResponse toFrontendResponse(ConversationContext context) {
    return StartConversationResponse.builder()
            .unbluConversationId(context.getUnbluConversationId())
            .unbluJoinUrl(context.getUnbluJoinUrl())
            .status("CREATED")
            .message("Conversation successfully created.")
            .build();
}
```
Ce POJO est ensuite automatiquement sérialisé en JSON par la configuration `bindingMode(RestBindingMode.json)` de Camel.

## 3. La réponse en cas de Refus (Moteur de Règles)
Si le moteur de règles refuse l'accès au Chat (par exemple, client dans un segment "BANNED"), nous déclenchons une exception métier `ChatAccessDeniedException`. 

Camel intercepte dynamiquement cette exception grâce au bloc `onException` configuré au début de la route, et force une réponse HTTP `403 Forbidden` avec un DTO formaté pour expliquer pourquoi :

```java
// Dans MainOrchestratorRoute.java :
onException(ChatAccessDeniedException.class)
    .handled(true) // Stoppe la propagation de l'erreur
    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403)) // HTTP 403
    .process(exchange -> {
        ChatAccessDeniedException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ChatAccessDeniedException.class);
        
        // Construction immédiate de la réponse de refus
        StartConversationResponse response = StartConversationResponse.builder()
                .status("REJECTED")
                .message(ex.getReason()) // ex: "Client blacklisté - Accès au chat refusé."
                .build();
                
        exchange.getIn().setBody(response);
    });
```

## Résumé
En résumé, l'application appelante recevra :
- Un json complet avec attribut `status: "CREATED"` (HTTP 200) si tout marche bien (ou si le mode hors-ligne s'active via le fallback).
- Un json partiel avec attribut `status: "REJECTED"` (HTTP 403) si le métier l'a interdit.
- Un json d'erreur Spring (HTTP 500) uniquement s'il y a un vrai crash technique non anticipé.
