package org.dbs.poc.unblu.infrastructure.orchestration;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.infrastructure.orchestration.dto.StartConversationRequest;
import org.dbs.poc.unblu.infrastructure.orchestration.dto.StartConversationResponse;
import org.dbs.poc.unblu.infrastructure.orchestration.mapper.DomainMapper;
import org.dbs.poc.unblu.infrastructure.orchestration.strategy.ErpContextEnricher;
import org.dbs.poc.unblu.infrastructure.orchestration.strategy.RuleEngineContextEnricher;
import org.springframework.stereotype.Component;

@Component
public class MainOrchestratorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Setup REST DSL configuration avec OpenAPI
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true")
            .apiContextPath("/openapi.json") // Point d'entrée pour la spec OpenAPI
            .apiProperty("api.title", "Unblu Camel Orchestrator API")
            .apiProperty("api.version", "1.0")
            .apiProperty("cors", "true");

        // Global Exception Handling : if Rule Engine denies access
        onException(ChatAccessDeniedException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403))
            .process(exchange -> {
                ChatAccessDeniedException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, ChatAccessDeniedException.class);
                StartConversationResponse response = StartConversationResponse.builder()
                        .status("REJECTED")
                        .message(ex.getReason())
                        .build();
                exchange.getIn().setBody(response);
            });

        // ==========================================
        // ROUTE PRINCIPALE / USE CASE
        // ==========================================
        rest("/v1/conversations").description("API d'orchestration de conversations Unblu")
            .post("/start")
                .description("Démarre une nouvelle conversation en orchestrant le contexte (ERP, Règles)")
                .consumes("application/json").produces("application/json")
                .type(StartConversationRequest.class).description("Données d'initialisation requises")
                .outType(StartConversationResponse.class)
                .responseMessage().code(200).message("Conversation initialisée ou Mode Hors-ligne activé").endResponseMessage()
                .responseMessage().code(403).message("Accès au chat refusé par le Moteur de Règles").endResponseMessage()
                .to("direct:start-conversation");

        from("direct:start-conversation")
            .routeId("main-conversation-orchestrator")

            // 1. Transformer la requête JSON en Objet du Domaine 'ConversationContext'
            .bean(DomainMapper.class, "initContext")

            // 2. Appel au système ERP (MOCK) -> Enrichit le profil client
            .enrich("direct:erp-adapter", new ErpContextEnricher())

            // 3. Appel au Moteur de Règles (MOCK) -> Enrichit la décision
            .enrich("direct:rule-engine-adapter", new RuleEngineContextEnricher())

            // 4. Décision Métier d'Orchestration (Autorisé ou Rejeté)
            .choice()
                .when(simple("${body.routingDecision.authorized} == false"))
                    .process(exchange -> {
                        // Throw custom exception based on rule engine reason
                        String reason = exchange.getIn().getBody(org.dbs.poc.unblu.domain.model.ConversationContext.class)
                                .getRoutingDecision().getRoutingReason();
                        throw new ChatAccessDeniedException("Accès refusé", reason);
                    })
                .otherwise()
                    // 5. Appel à l'API Unblu avec Resilience4j (Circuit Breaker)
                    .circuitBreaker()
                        // Configuration optionnelle inline (sinon utilise le application.properties)
                        .resilience4jConfiguration().timeoutEnabled(true).timeoutDuration(3000).end()
                        .to("direct:unblu-adapter")
                    .onFallback()
                        // Stratégie de repli si l'API est injoignable ou timeout
                        .log("⚠️ L'API Unblu est injoignable ou a expiré. Déclenchement du Fallback.")
                        .process(exchange -> {
                            org.dbs.poc.unblu.domain.model.ConversationContext ctx = 
                                    exchange.getIn().getBody(org.dbs.poc.unblu.domain.model.ConversationContext.class);
                            ctx.setUnbluConversationId("OFFLINE-PENDING");
                            ctx.setUnbluJoinUrl("Le service de chat est temporairement indisponible.");
                            exchange.getIn().setBody(ctx);
                        })
                    .end() // Fin du CircuitBreaker
            .end() // Fin du Choice

            // 6. Transformer le 'ConversationContext' final en 'StartConversationResponse' pour le client
            .bean(DomainMapper.class, "toFrontendResponse");
    }
}
