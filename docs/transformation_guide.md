# Guide de Transformation : De l'Architecture Hexagonale Stricte à l'Architecture Pragmatique (Camel-Centric)

Ce document est destiné à un agent IA pour transformer un projet Unblu d'une architecture hexagonale "stricte" (où Camel est uniquement en infrastructure) vers une architecture "pragmatique" (où Camel orchestre les Use Cases dans la couche application).

## 1. Principes de l'Architecture Cible
- **Orchestration Centralisée** : Toute la logique de flux (ERP -> Rule Engine -> Unblu) est déplacée des services Java vers une route Camel `MainOrchestratorRoute` dans `unblu-application`.
- **Services Pass-Through** : Les services du domaine/application (ex: `ConversationOrchestratorService`) deviennent de simples proxys appelant Camel via `ProducerTemplate`.
- **Adaptateurs d'Infrastructure** : Les adaptateurs d'infrastructure (ex: `UnbluCamelAdapter`) doivent exposer des endpoints `direct:` pour être appelés par l'orchestrateur.
- **Objet Pivot** : Utilisation systématique de `ConversationContext` comme objet de transfert entre les étapes du flux.

---

## 2. Étapes de Transformation

### Étape 1 : Mise à jour des Dépendances
Dans le module `unblu-application/pom.xml`, ajoutez le support Camel :
```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
```

### Étape 2 : Création de la Route d'Orchestration principale
Créez `org.dbs.poc.unblu.application.service.MainOrchestratorRoute` dans `unblu-application`.

**Points clés à implémenter :**
- Utilisez `.enrich()` pour appeler les adaptateurs ERP et Rule Engine sans perdre le `ConversationContext`.
- Utilisez `.choice()` pour implémenter les décisions métiers (ex: `ChatAccessDeniedException`).
- Sauvegardez la commande initiale dans une propriété Camel (`originalCommand`) pour y accéder plus tard dans le flux si le body change.

### Étape 3 : Simplification des Services d'Application
Modifiez les services (ex: `ConversationOrchestratorService`, `DirectConversationService`) pour supprimer la logique procédurale.

**Modèle de code :**
```java
@Service
@RequiredArgsConstructor
public class MyService implements MyUseCase {
    private final ProducerTemplate producerTemplate;

    @Override
    public Result execute(Command command) {
        return producerTemplate.requestBody("direct:start-my-flow", command, Result.class);
    }
}
```

### Étape 4 : Adaptation de l'Infrastructure
Assurez-vous que tous les adaptateurs d'infrastructure (`unblu-infrastructure`) héritent de `RouteBuilder` et exposent leurs fonctionnalités via des endpoints `direct:`.

- `direct:erp-adapter`
- `direct:rule-engine-adapter`
- `direct:unblu-adapter-resilient`
- `direct:unblu-search-persons`
- `direct:conversation-summary-adapter`

### Étape 5 : Gestion des Erreurs et Résilience
- Implémentez la résilience (Circuit Breaker) dans `UnbluResilientRoute` au niveau de l'infrastructure.
- Gérez les exceptions métiers (`ChatAccessDeniedException`) directement dans la route DSL Camel via `.process()` ou `onException`.

---

## 3. Patterns de Transformation Critique (Exemples)

### Mapping de l'Objet Pivot dans Camel
```java
.enrich("direct:erp-adapter", (oldExchange, newExchange) -> {
    ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
    ctx.setCustomerProfile(newExchange.getIn().getBody(org.dbs.poc.unblu.domain.model.CustomerProfile.class));
    return oldExchange;
})
```

### Accès aux données de la commande initiale
```java
.setProperty("originalCommand", body())
// ... plus tard dans la route ...
.process(exchange -> {
    StartDirectConversationCommand cmd = exchange.getProperty("originalCommand", StartDirectConversationCommand.class);
    // Logique utilisant cmd.getVirtualParticipantSourceId()
})
```

---

## 4. Implémentations de Référence Complètes

Pour garantir une transformation sans erreur de compilation, voici les fichiers sources complets de la branche `pragmatique-architecture`.

### A. MainOrchestratorRoute.java
Ce fichier contient toute la logique d'orchestration (séquençage, enrichissement, décisions métiers).

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartDirectConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainOrchestratorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // ==========================================
        // ROUTE PRINCIPALE : Start Conversation (USE CASE)
        // ==========================================
        from("direct:start-conversation")
            .routeId("main-orchestrator-start-conversation")
            .log("Démarrage de l'orchestration Camel pour clientId: ${body.clientId}")
            
            // 1. Initialisation du Contexte (Objet Pivot)
            .process(exchange -> {
                StartConversationCommand command = exchange.getIn().getBody(StartConversationCommand.class);
                ConversationContext context = ConversationContext.builder()
                        .initialClientId(command.getClientId())
                        .originApplication(command.getOrigin())
                        .build();
                exchange.getIn().setBody(context);
            })

            // 2. Appel ERP (Adapter)
            .enrich("direct:erp-adapter", (oldExchange, newExchange) -> {
                ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
                ctx.setCustomerProfile(newExchange.getIn().getBody(org.dbs.poc.unblu.domain.model.CustomerProfile.class));
                return oldExchange;
            })

            // 3. Appel Moteur de Règles (Adapter)
            .enrich("direct:rule-engine-adapter", (oldExchange, newExchange) -> {
                ConversationContext ctx = oldExchange.getIn().getBody(ConversationContext.class);
                ctx.setRoutingDecision(newExchange.getIn().getBody(org.dbs.poc.unblu.domain.model.ChatRoutingDecision.class));
                return oldExchange;
            })

            // 4. Décision Métier d'Orchestration (DSL Camel)
            .choice()
                .when(simple("${body.routingDecision.authorized} == false"))
                    .log("Accès refusé par le moteur de règles")
                    .process(exchange -> {
                        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                        throw new ChatAccessDeniedException("Accès refusé", ctx.getRoutingDecision().getRoutingReason());
                    })
                .otherwise()
                    // 5. Appel Unblu (Adapter Resilient)
                    .to("direct:unblu-adapter-resilient")
                    
                    // 6. Ajout du résumé (Post-process)
                    .to("direct:unblu-add-summary-internal")
            .end();

        // ==========================================
        // ROUTE : Start Direct Conversation (USE CASE)
        // ==========================================
        from("direct:start-direct-conversation")
            .routeId("main-orchestrator-start-direct-conversation")
            .log("Démarrage d'une conversation directe - VIRTUAL: ${body.virtualParticipantSourceId}")
            
            // Sauvegarde de la commande car le Body va changer
            .setProperty("originalCommand", body())

            // 1. Résolution Participant Virtual
            .process(exchange -> {
                StartDirectConversationCommand cmd = exchange.getProperty("originalCommand", StartDirectConversationCommand.class);
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.PersonSearchRequest(cmd.getVirtualParticipantSourceId(), org.dbs.poc.unblu.domain.model.PersonSource.VIRTUAL));
            })
            .enrich("direct:unblu-search-persons", (oldEx, newEx) -> {
                java.util.List<org.dbs.poc.unblu.domain.model.PersonInfo> persons = newEx.getIn().getBody(java.util.List.class);
                if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("VIRTUAL introuvable");
                oldEx.setProperty("virtualPerson", persons.getFirst());
                return oldEx;
            })
            
            // 2. Appel ERP
            .process(exchange -> {
                StartDirectConversationCommand cmd = exchange.getProperty("originalCommand", StartDirectConversationCommand.class);
                ConversationContext context = ConversationContext.builder()
                        .initialClientId(cmd.getVirtualParticipantSourceId())
                        .build();
                exchange.getIn().setBody(context);
            })
            .enrich("direct:erp-adapter", (oldEx, newEx) -> {
                ConversationContext ctx = oldEx.getIn().getBody(ConversationContext.class);
                ctx.setCustomerProfile(newEx.getIn().getBody(org.dbs.poc.unblu.domain.model.CustomerProfile.class));
                return oldEx;
            })
            
            // 3. Rule Engine
            .enrich("direct:rule-engine-adapter", (oldEx, newEx) -> {
                ConversationContext ctx = oldEx.getIn().getBody(ConversationContext.class);
                ctx.setRoutingDecision(newEx.getIn().getBody(org.dbs.poc.unblu.domain.model.ChatRoutingDecision.class));
                
                if (!ctx.getRoutingDecision().isAuthorized()) {
                    throw new ChatAccessDeniedException("Accès refusé", ctx.getRoutingDecision().getRoutingReason());
                }
                return oldEx;
            })
            
            // 4. Résolution Agent
            .process(exchange -> {
                StartDirectConversationCommand cmd = exchange.getProperty("originalCommand", StartDirectConversationCommand.class);
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.PersonSearchRequest(cmd.getAgentParticipantSourceId(), org.dbs.poc.unblu.domain.model.PersonSource.USER_DB));
            })
            .enrich("direct:unblu-search-persons", (oldEx, newEx) -> {
                java.util.List<org.dbs.poc.unblu.domain.model.PersonInfo> persons = newEx.getIn().getBody(java.util.List.class);
                if (persons == null || persons.isEmpty()) throw new IllegalArgumentException("Agent introuvable");
                oldEx.setProperty("agentPerson", persons.getFirst());
                return oldEx;
            })
            
            // 5. Création Conversation Directe
            .process(exchange -> {
                StartDirectConversationCommand cmd = exchange.getProperty("originalCommand", StartDirectConversationCommand.class);
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.DirectConversationRequest(
                        exchange.getProperty("virtualPerson", org.dbs.poc.unblu.domain.model.PersonInfo.class),
                        exchange.getProperty("agentPerson", org.dbs.poc.unblu.domain.model.PersonInfo.class),
                        cmd.getSubject()));
            })
            .to("direct:unblu-create-direct-conversation")
            
            // 6. Résumé (Post-process)
            .process(exchange -> {
                com.unblu.webapi.model.v4.ConversationData data = exchange.getIn().getBody(com.unblu.webapi.model.v4.ConversationData.class);
                exchange.setProperty("convId", data.getId());
            })
            .toD("direct:conversation-summary-adapter")
            .process(exchange -> {
                String summary = exchange.getIn().getBody(String.class);
                String convId = exchange.getProperty("convId", String.class);
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest(convId, summary));
            })
            .to("direct:unblu-add-summary")
            .process(exchange -> {
                 String convId = exchange.getProperty("convId", String.class);
                 exchange.getIn().setBody(new org.dbs.poc.unblu.domain.model.UnbluConversationInfo(convId, convId));
            });

        // Sous-route interne pour l'ajout du résumé
        from("direct:unblu-add-summary-internal")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                exchange.setProperty("convId", ctx.getUnbluConversationId());
            })
            .toD("direct:conversation-summary-adapter")
            .process(exchange -> {
                String summary = exchange.getIn().getBody(String.class);
                String convId = exchange.getProperty("convId", String.class);
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest(convId, summary));
            })
            .to("direct:unblu-add-summary");
    }
}
```

### B. ConversationOrchestratorService.java
Exemple de service réduit à un simple appel vers la route Camel.

```java
package org.dbs.poc.unblu.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.application.port.in.StartConversationUseCase;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {

    private final ProducerTemplate producerTemplate;

    @Override
    public ConversationContext startConversation(StartConversationCommand command) {
        log.info("Appel de l'orchestrateur Camel (Pragmatic) pour clientId: {}", command.getClientId());
        return producerTemplate.requestBody("direct:start-conversation", command, ConversationContext.class);
    }
}
```

---

## 5. Checklist de Vérification
1. [ ] `unblu-application` compile avec la dépendance Camel.
2. [ ] Les services Java n'ont plus de logique de "if/else" ou d'appels séquentiels à plusieurs ports.
3. [ ] `MainOrchestratorRoute` contient l'enchaînement complet des appels.
4. [ ] Les exceptions jetées dans Camel sont correctement remontées jusqu'au contrôleur REST.
5. [ ] `mvn clean compile` passe sur tout le projet.
