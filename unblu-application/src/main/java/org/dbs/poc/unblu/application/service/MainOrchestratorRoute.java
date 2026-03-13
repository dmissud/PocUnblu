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
                    .enrich("direct:conversation-summary-adapter", (oldExchange, newExchange) -> {
                        // Le summary adapter attend l'ID de conversation
                        return oldExchange; // On garde le contexte enrichi
                    })
                    .to("direct:unblu-add-summary-internal")
            .end();

        // ==========================================
        // ROUTE : Start Direct Conversation (USE CASE)
        // ==========================================
        from("direct:start-direct-conversation")
            .routeId("main-orchestrator-start-direct-conversation")
            .log("Démarrage d'une conversation directe - VIRTUAL: ${body.virtualParticipantSourceId}")
            
            // On sauvegarde le command initial dans une propriété car le Body va changer
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
            
            // 2. Appel ERP (Réutilisation Adapter)
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
            // Le body est déjà un ConversationContext enrichi par l'enrich de l'ERP
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

        // Sous-route interne pour l'ajout du résumé (facilite le mapping)
        from("direct:unblu-add-summary-internal")
            .process(exchange -> {
                ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
                // On prépare l'appel pour l'adaptateur summary
                exchange.setProperty("convId", ctx.getUnbluConversationId());
            })
            .toD("direct:conversation-summary-adapter") // On récupère le résumé
            .process(exchange -> {
                String summary = exchange.getIn().getBody(String.class);
                String convId = exchange.getProperty("convId", String.class);
                // On prépare l'objet SummaryRequest pour l'adaptateur Unblu
                exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest(convId, summary));
            })
            .to("direct:unblu-add-summary");
    }
}
