package org.dbs.poc.unblu.application.service;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.dbs.poc.unblu.application.port.in.StartConversationCommand;
import org.dbs.poc.unblu.domain.model.ChatAccessDeniedException;
import org.dbs.poc.unblu.domain.model.ConversationContext;
import org.dbs.poc.unblu.domain.model.CustomerProfile;
import org.dbs.poc.unblu.domain.model.ChatRoutingDecision;
import org.springframework.stereotype.Component;

import static org.dbs.poc.unblu.application.service.OrchestratorEndpoints.*;

@Component
public class StartConversationRoute extends RouteBuilder {

    @Override
    public void configure() {
        from(DIRECT_START_CONVERSATION)
            .routeId("main-orchestrator-start-conversation")
            .log("Démarrage de l'orchestration Camel")

            .process(this::initConversationContext)
            .log("Context initialisé, appel de l'adapter Unblu pour créer la conversation")
            .to(DIRECT_UNBLU_ADAPTER_RESILIENT)
            .log("Conversation créée, enrichissement avec le summary")
            .enrich(DIRECT_CONVERSATION_SUMMARY_ADAPTER, (oldExchange, newExchange) -> oldExchange)
            .to(DIRECT_UNBLU_ADD_SUMMARY_INTERNAL);

        from(DIRECT_UNBLU_ADD_SUMMARY_INTERNAL)
            .routeId("main-orchestrator-add-summary-internal")
            .process(this::prepareSummaryRequestFromContext)
            .log("Génération du summary pour la conversation ID: ${exchangeProperty.convId}")
            .enrich(DIRECT_CONVERSATION_SUMMARY_ADAPTER, this::aggregateSummary)
            .log("Summary généré: ${exchangeProperty.summary}")
            .process(this::prepareFinalSummaryRequest)
            .to(DIRECT_UNBLU_ADD_SUMMARY)
            .log("Summary ajouté à la conversation")
            .process(this::restoreContext);
    }

    private void initConversationContext(Exchange exchange) {
        StartConversationCommand command = exchange.getIn().getBody(StartConversationCommand.class);
        ConversationContext context = new ConversationContext(
                command.clientId(),
                command.origin());

        // Crée une décision de routage directement avec le teamId fourni
        ChatRoutingDecision decision = new ChatRoutingDecision(
                true,
                command.teamId(),
                "Team fournie par le front"
        );
        context.setRoutingDecision(decision);

        exchange.getIn().setBody(context);
    }

    private void prepareSummaryRequestFromContext(Exchange exchange) {
        ConversationContext ctx = exchange.getIn().getBody(ConversationContext.class);
        exchange.setProperty("convId", ctx.getUnbluConversationId());
        exchange.setProperty("conversationContext", ctx);
    }

    private Exchange aggregateSummary(Exchange oldExchange, Exchange newExchange) {
        String summary = newExchange.getIn().getBody(String.class);
        oldExchange.setProperty("summary", summary);
        return oldExchange;
    }

    private void prepareFinalSummaryRequest(Exchange exchange) {
        String summary = exchange.getProperty("summary", String.class);
        String convId = exchange.getProperty("convId", String.class);
        exchange.getIn().setBody(new org.dbs.poc.unblu.infrastructure.adapter.unblu.UnbluCamelAdapterPort.SummaryRequest(convId, summary));
    }

    private void restoreContext(Exchange exchange) {
        ConversationContext ctx = exchange.getProperty("conversationContext", ConversationContext.class);
        exchange.getIn().setBody(ctx);
    }
}
