### Activation d'une route Camel via REST et Kafka

Pour activer une même logique métier (une route d'orchestration) via deux canaux différents (REST et Kafka), la meilleure pratique Camel consiste à utiliser un point d'entrée commun via un endpoint `direct:`.

#### Architecture recommandée
1.  **Route d'Orchestration Commune** : Elle contient la logique métier réelle et commence par `from("direct:ma-logique-metier")`.
2.  **Route d'Exposition REST** : Elle expose l'API HTTP et transfère le message vers la route commune.
3.  **Route de Consommation Kafka** : Elle écoute le topic Kafka, transforme éventuellement le message (désérialisation JSON), puis le transfère vers la même route commune.

#### Exemple d'implémentation (Java DSL)

Voici comment structurer cela dans une classe `RouteBuilder` :

```java
@Component
public class IngestionOrchestratorRoute extends RouteBuilder {

    @Override
    public void configure() {
        // 1. Point d'entrée REST (via le composant Camel REST)
        rest("/api/conversations")
            .post("/start")
            .type(StartConversationCommand.class) // Mapping automatique du JSON
            .to(DIRECT_START_CONVERSATION_CORE);

        // 2. Point d'entrée Kafka
        from("kafka:topic-unblu-events?brokers=localhost:9092")
            .routeId("kafka-consumer")
            .unmarshal().json(JsonLibrary.Jackson, StartConversationCommand.class) // Désérialisation
            .process(this::logKafkaEvent)
            .to(DIRECT_START_CONVERSATION_CORE);

        // 3. LA LOGIQUE COMMUNE (Orchestration)
        from(DIRECT_START_CONVERSATION_CORE)
            .routeId("common-orchestrator-logic")
            .process(this::initContext)
            .enrich(DIRECT_ERP_ADAPTER, this::aggregateCustomerProfile)
            .to(DIRECT_UNBLU_ADAPTER_RESILIENT);
    }
    
    private void logKafkaEvent(Exchange exchange) {
        // Logique spécifique aux événements Kafka si besoin
    }
}
```

#### Points clés
- **Découplage** : La logique d'orchestration ne sait pas si le message vient de Kafka ou d'une API. Elle manipule un objet pivot (comme un `Record` Java).
- **Transformation** : La route Kafka s'occupe de rendre le message compatible avec le format attendu par l'orchestrateur (via `unmarshal` ou un `Processor`).
- **Consistance** : Toutes les constantes d'endpoints (ex: `DIRECT_START_CONVERSATION_CORE`) doivent être centralisées dans votre interface `OrchestratorEndpoints` pour garantir la maintenabilité.

Cette approche a été ajoutée au [Guide de Transformation](docs/transformation_guide.md) dans la section **C. Multi-Point d'Entrée**.