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

### Étape 1 : Support Camel dans l'Application et l'Exposition
Dans les modules `unblu-application/pom.xml` et `unblu-exposition/pom.xml` :
```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
</dependency>
<!-- Dans exposition, ajoutez aussi camel-jackson-starter pour le binding JSON -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-jackson-starter</artifactId>
</dependency>
```

### Étape 2 : Définition des Endpoints
Créez `OrchestratorEndpoints.java` pour centraliser les constantes.

### Étape 3 : Migration de l'Exposition (REST DSL)
Remplacez les `@RestController` Spring par une classe `RestExpositionRoute` dans le module `unblu-exposition`.

```java
@Component
public class RestExpositionRoute extends RouteBuilder {
    @Override
    public void configure() {
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "Unblu Camel API")
            .apiProperty("api.version", "1.0.0");

        rest("/v1/conversations")
            .post("/start")
                .type(StartConversationRequest.class)
                .outType(StartConversationResponse.class)
                .to("direct:rest-start-conversation");

        // Redirection par défaut vers la doc API
        rest("/")
            .get()
                .to("direct:redirect-to-doc");

        from("direct:redirect-to-doc")
            .routeId("redirect-to-doc")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(302))
            .setHeader("Location", constant("/api/api-doc"))
            .setBody(constant(""));

        from("direct:rest-start-conversation")
            .process(this::mapToCommand)
            .to(OrchestratorEndpoints.DIRECT_START_CONVERSATION)
            .process(this::mapToResponse);
    }
}
```

### Étape 4 : Documentation API (OpenAPI)
L'architecture pragmatique utilise `camel-openapi-java` au lieu de `springdoc`.
- **Accès** : La documentation est accessible via le point d'entrée configuré (ex: `/api/api-doc`).
- **Configuration** : Tout se passe dans la `restConfiguration()` de la route d'exposition.

### Étape 5 : Création des Routes d'Orchestration Spécialisées

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

### C. Multi-Point d'Entrée (REST & Kafka)
Pour activer une logique métier via plusieurs canaux (ex: Appel API et Message Kafka), utilisez un endpoint `direct:` commun.

**Architecture recommandée :**
1. **Route d'Orchestration (Cœur)** : Consomme depuis `direct:process-data`.
2. **Route d'Exposition REST** : Expose un endpoint HTTP et transfère vers `direct:process-data`.
3. **Route d'Événement Kafka** : Écoute un topic et transfère vers `direct:process-data` après transformation.

```java
@Component
public class DataIngestionRoute extends RouteBuilder {
    @Override
    public void configure() {
        // 1. Entrée REST (Composant Camel REST)
        rest("/api/data")
            .post()
            .type(DataRequest.class)
            .to(DIRECT_PROCESS_DATA);

        // 2. Entrée Kafka
        from("kafka:topic-data?brokers=localhost:9092")
            .routeId("kafka-consumer-route")
            .unmarshal().json(JsonLibrary.Jackson, DataRequest.class)
            .to(DIRECT_PROCESS_DATA);

        // 3. Orchestration commune
        from(DIRECT_PROCESS_DATA)
            .routeId("common-orchestrator")
            .process(this::enrichData)
            .to(DIRECT_DB_ADAPTER);
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
