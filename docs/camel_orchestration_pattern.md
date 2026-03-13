# Pattern de Conception : Orchestration Résiliente avec Apache Camel & Architecture Hexagonale

Ce document décrit les principes avancés de mise en œuvre d'Apache Camel au sein d'une architecture Hexagonale. Il intègre notamment les concepts de résilience (Retry, Circuit Breaker) indispensables lors de l'orchestration de systèmes externes (ERP, Moteur de Règles, Unblu).

---

## 1. Cartographie Hexagonale & Apache Camel

Dans une architecture hexagonale stricte, le domaine métier ne doit avoir aucune dépendance technique (ni Spring, ni HTTP, ni Camel). Camel se positionne principalement dans la couche **Infrastructure** (les Adapters).

Cependant, pour un composant dont le cœur de métier *est* l'orchestration (API Gateway, BFF), l'approche **Pragmatique Hexamodulaire** consiste à considérer la déclaration des Routes principales Camel comme la définition même du *Use Case*.

| Concept Hexagonal | Traduction dans ce pattern avec Apache Camel |
| :--- | :--- |
| **Domaine (Modèles)** | POJOs purs (Java). Ex: `ConversationContext`, `EligibilityStatus`. L'objet pivote au centre de la route. |
| **Ports Primaires (Inbound)** | Points d'entrée de la route principale. Ex: Déclencheur REST (`rest()`), Kafka consumer. |
| **Ports Secondaires (Outbound)** | Interfaces Java décrivant le besoin du domaine. Ex: `ErpProvider`, `UnbluGateway`. |
| **Adapters Secondaires (Driven)**| **Sous-routes Camel** (`direct:xyz`) chargées de la tuyauterie HTTP, de l'authentification et de la **résilience** (Circuit Breaker, Retry), ou implémentations Java du port appelant un `ProducerTemplate` Camel. |
| **Use Case (Service)** | La route Camel principale (le Chef d'Orchestre) qui définit le flux (Appel A $\rightarrow$ Enrichir $\rightarrow$ Appel B $\rightarrow$ Condition $\rightarrow$ Appel C). |

---

## 2. Piliers de la Résilience (Retry & Circuit Breaker)

Lors de l'appel à des systèmes externes comme un ERP ou un moteur de règles, Camel offre des mécanismes natifs pour éviter la propagation des pannes (Cascading Failures) :

*   **Retry (onException) :** On gère les erreurs transitoires (Timeouts de connexion brefs). Camel retente l'appel un nombre fini de fois avant d'abandonner.
*   **Circuit Breaker (Resilience4j) :** Si l'ERP est totalement tombé, il est inutile de s'acharner (ce qui saturerait nos propres threads). Le disjoncteur s'ouvre : les appels échouent instantanément (Fail-fast) ou déclenchent un comportement de secours (*Fallback*).

---

## 3. Implémentation Complète : L'Orchestrateur Unblu

Voici l'architecture projetée :

1. L'appel REST déclenche l'orchestration.
2. Appel à l'**ERP** (Récupération de données client) avec **Circuit Breaker** (si ERP down $\rightarrow$ profil invité/anonyme).
3. Appel au **Moteur de Règles** avec **Retry** (si timeout) et **Circuit Breaker** (si down $\rightarrow$ accès refusé par défaut).
4. Création dans **Unblu**.

### A. Le Domaine (Les Objets Pivots purs)

```java
// domain/model/ConversationContext.java
public class ConversationContext {
    private String clientId;
    private CustomerProfile customerProfile; // Enrichi par l'ERP
    private ChatRoutingDecision routingDecision;  // Enrichi par le Moteur
    private String unbluConversationId;      // Résultat final
    // getters/setters...
}
```

### B. Le Use Case : La Route Principale (Orchestrateur)

Cette route définit le flux métier de manière lisible. Toute la complexité technique et la gestion des pannes sont déléguées aux sous-routes.

```java
// infrastructure/orchestration/MainOrchestratorRoute.java
@Component
public class MainOrchestratorRoute extends RouteBuilder {

    @Override
    public void configure() {
        
        // Gestion des exceptions métier remontées
        onException(ChatAccessDeniedException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403))
            .setBody(constant("Accès au chat refusé par les règles métier."));

        // Point d'entrée (Port Primaire)
        rest("/api/conversations").post("/unblu")
            .type(StartChatRequest.class)
            .route().routeId("main-unblu-orchestrator")
            
            // 1. Initialiser le Contexte Pivot
            .bean(DomainMapper.class, "initContext")
            
            // 2. Appel ERP
            // Si le Circuit Breaker de l'ERP s'ouvre, le Fallback mettra un "Profil Défaut"
            .enrich("direct:erp-adapter", new ErpContextEnricher())
            
            // 3. Appel Moteur de Règles
            .enrich("direct:rule-engine-adapter", new RuleEngineContextEnricher())
            
            // 4. Décision Métier (le Moteur a-t-il dit oui ?)
            .choice()
                .when(simple("${body.routingDecision.authorized} == false"))
                    .throwException(ChatAccessDeniedException.class)
                .otherwise()
                    // 5. Appel Unblu
                    .to("direct:unblu-adapter")
            .end()
            
            // 6. Mapping réponse frontend
            .bean(DomainMapper.class, "toFrontendResponse");
    }
}
```

### C. Les Adapters Secondaires avec Résilience

C'est ici que l'on implémente la tuyauterie technique (Headers, Mappings JSON bruts) et les stratégies de sécurité (Resilience4j).

```java
// infrastructure/adapters/ExternalSystemsCamelAdapters.java
@Component
public class ExternalSystemsCamelAdapters extends RouteBuilder {

    @Override
    public void configure() {

        // ==========================================
        // ADAPTER 1 : ERP (Avec Circuit Breaker et Fallback)
        // ==========================================
        from("direct:erp-adapter")
            .routeId("erp-system-adapter")
            // Utilisation du Circuit Breaker Resilience4j
            .circuitBreaker()
                // Configuration de Resilience4j (souvent défini dans application.yml)
                .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(2000).end()
                
                // Préparation JSON et appel HTTP technique
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader(Exchange.HTTP_PATH, simple("/clients/profile/${body.clientId}"))
                .to("http://system-erp-legacy:8080")
                .unmarshal().json(JsonLibrary.Jackson, ErpResponseDto.class)
            
            // FALLBACK : Si l'ERP ne répond pas (Timeout) ou est disjoncté (Open)
            .onFallback()
                .log("⚠️ ERP indisponible. Utilisation d'un profil par défaut pour clientId: ${body.clientId}")
                .bean(FallbackGenerator.class, "generateDefaultGuestProfile")
            .end();


        // ==========================================
        // ADAPTER 2 : Moteur de Règles (Avec Retry et Circuit Breaker Strict)
        // ==========================================
        // On gère spécifiquement les erreurs réseau (SocketTimeout)
        onException(java.net.SocketTimeoutException.class)
            .maximumRedeliveries(3)          // 3 tentatives
            .redeliveryDelay(500)            // Attendre 500ms entre chaque
            .retryAttemptedLogLevel(LoggingLevel.WARN)
            .handled(false); // Laisse propager l'erreur si les 3 essais échouent (déclenchera le CB)

        from("direct:rule-engine-adapter")
            .routeId("rule-engine-adapter")
            // Transformer le "ConversationContext" en DTO pour le moteur de règles
            .bean(RuleEngineMapper.class, "toEngineRequest")
            .marshal().json(JsonLibrary.Jackson)
            
            .circuitBreaker()
                // Si le moteur tombe, on ne peut ABSOLUMENT PAS autoriser le chat. (Pas de fallback complaisant)
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://rule-engine:8080/evaluate")
                .unmarshal().json(JsonLibrary.Jackson, RuleEngineDecisionDto.class)
            .onFallback()
                .log("🚨 Moteur de règles HS. Blocage préventif de l'accès.")
                .bean(FallbackGenerator.class, "generateDenyAllRoutingDecision")
            .end();


        // ==========================================
        // ADAPTER 3 : Unblu Gateway
        // ==========================================
        from("direct:unblu-adapter")
            .routeId("unblu-rest-adapter")
            .bean(UnbluMapper.class, "toUnbluCreateConversationRequest")
            .marshal().json(JsonLibrary.Jackson)
            .setHeader("x-unblu-apikey", simple("{{api.keys.unblu}}"))
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .to("https://api.unblu.com/unblu/rest/v3/conversations/create")
            .unmarshal().json(JsonLibrary.Jackson, UnbluResponseDto.class)
            // Mise à jour de l'objet métier de la route principale
            .bean(UnbluMapper.class, "enrichContextWithUnbluId");
    }
}
```

---

## 4. Synthèse des Stratégies de Fallback (Secours)

La notion de Circuit Breaker implique que le développeur doit réfléchir à l'action de secours (*Fallback*). Dans notre architecture hexagonale, l'impact sur le modèle métier dépend de la criticité du système externe de secours :

1.  **ERP est "Nice to Have" (Bonus d'information) :** 
    *   *Stratégie :* Le fallback de l'ERP renvoie un objet `CustomerProfile` "Vide" ou "Invité". La route continue. Le moteur de règle prendra sa décision basée sur le profil invité.
2.  **Moteur de Règles est "Mandatory" (Critique pour la sécurité/compliance) :**
    *   *Stratégie :* Le fallback renvoie un `ChatRoutingDecision` avec `authorized = false`. La route principale s'arrêtera au `choice()` et renverra une erreur 403, mais le composant ne plantera pas et informera l'utilisateur proprement ("Service de chat momentanément indisponible").

En séparant ainsi clairement l'orchestration principale (route A) de l'implémentation technique des adaptateurs avec leur tuyauterie de résilience (routes B, C, D), vous respectez l'esprit de l'Architecture Hexagonale (Lisibilité, Maintenabilité) tout en exploitant 100% de la puissance d'Apache Camel.
