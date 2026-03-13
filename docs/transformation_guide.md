# Guide de Transformation : Architecture Pragmatique et Clean Code (Camel-Centric)

Ce document est destiné à un agent IA pour transformer un projet Unblu d'une architecture hexagonale "stricte" vers une architecture "pragmatique" hautement maintenable.

## 1. Principes de l'Architecture Cible
- **Orchestration Modulaire** : La logique de flux est découpée en `RouteBuilder` spécialisés par Use Case (ex: `StartConversationRoute`, `StartDirectConversationRoute`) dans `unblu-application`.
- **Centralisation des Endpoints** : Utilisation de l'interface `OrchestratorEndpoints` pour toutes les URIs Camel.
- **Modèle Riche (Records)** : Utilisation de Java `Records` pour l'immuabilité et la validation (Commands, DTOs).
- **Objet Pivot** : Utilisation de `ConversationContext` pour le transfert d'état entre les étapes techniques.
- **Clean Code** : Extraction systématique des `Processors` et `Aggregators` vers des méthodes privées nommées pour un DSL Camel auto-documenté.

---

## 2. Étapes de Transformation

### Étape 1 : Support Camel dans l'Application
Dans le module `unblu-application/pom.xml` :
```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
```

### Étape 2 : Définition des Endpoints
Créez `OrchestratorEndpoints.java` pour centraliser les constantes :
```java
public interface OrchestratorEndpoints {
    String DIRECT_START_CONVERSATION = "direct:start-conversation";
    String DIRECT_ERP_ADAPTER = "direct:erp-adapter";
    // ...
}
```

### Étape 3 : Création des Routes Spécialisées
Ne créez pas une seule route monolithique. Séparez chaque Use Case.

**Règles de Clean Code pour les routes :**
- Utilisez `this::methodName` pour les processeurs et agrégateurs.
- Nommez vos variables de lambda de manière explicite (ex: `teamInfo` au lieu de `t`).
- Sauvegardez le contexte initial dans des propriétés Camel (`setProperty`) si le corps (`body`) change.

### Étape 4 : Modélisation Riche
Remplacez les classes anémiques par des `Records` avec validation :
```java
public record StartConversationCommand(String clientId, String origin) {
    public StartConversationCommand {
        Objects.requireNonNull(clientId);
    }
}
```

---

## 3. Implémentations de Référence

### A. Route d'Orchestration Modulaire (Exemple)
```java
@Component
public class StartConversationRoute extends RouteBuilder {
    @Override
    public void configure() {
        from(DIRECT_START_CONVERSATION)
            .routeId("start-conversation-use-case")
            .process(this::initContext)
            .enrich(DIRECT_ERP_ADAPTER, this::aggregateCustomerProfile)
            .choice()
                .when(simple("${body.chatAuthorized} == false"))
                    .throwException(new ChatAccessDeniedException(...))
                .otherwise()
                    .to(DIRECT_UNBLU_ADAPTER_RESILIENT)
            .end();
    }

    private void initContext(Exchange exchange) {
        StartConversationCommand cmd = exchange.getIn().getBody(StartConversationCommand.class);
        exchange.getIn().setBody(new ConversationContext(cmd.clientId(), cmd.origin()));
    }

    private Exchange aggregateCustomerProfile(Exchange oldEx, Exchange newEx) {
        ConversationContext ctx = oldEx.getIn().getBody(ConversationContext.class);
        ctx.setCustomerProfile(newEx.getIn().getBody(CustomerProfile.class));
        return oldEx;
    }
}
```

### B. Service Applicatif (Proxy)
```java
@Service
@RequiredArgsConstructor
public class ConversationOrchestratorService implements StartConversationUseCase {
    private final ProducerTemplate producerTemplate;

    @Override
    public ConversationContext startConversation(StartConversationCommand command) {
        return producerTemplate.requestBody(DIRECT_START_CONVERSATION, command, ConversationContext.class);
    }
}
```

---

## 4. Checklist de Migration
1. [ ] Migration des Commands/Queries vers des Java `Records`.
2. [ ] Centralisation des URIs dans `OrchestratorEndpoints`.
3. [ ] Création des classes `RouteBuilder` par Use Case.
4. [ ] Suppression de la logique procédurale dans les services Java.
5. [ ] Validation du build via `mvn clean compile`.
